package main

import (
  "database/sql"
  "encoding/json"
  "fmt"
  "log"
  "net/http"
  "os"
  "time"

  _ "github.com/mattn/go-sqlite3"
  amqp "github.com/rabbitmq/amqp091-go"
)

type OrderEvent struct { OrderID int64 `json:"orderId"`; CustomerID string `json:"customerId"`; ProductID int64 `json:"productId"`; Quantity int `json:"quantity"` }
type Invoice struct { ID int64 `json:"id"`; OrderID int64 `json:"orderId"`; Status string `json:"status"` }
var db *sql.DB

func main() {
  var err error
  db, err = sql.Open("sqlite3", "/data/billing.db")
  if err != nil { log.Fatal(err) }
  _, err = db.Exec(`CREATE TABLE IF NOT EXISTS invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER UNIQUE, status TEXT NOT NULL, created_at TEXT NOT NULL)`)
  if err != nil { log.Fatal(err) }
  go consume()
  http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) { json.NewEncoder(w).Encode(map[string]string{"status":"UP", "service":"billing-service"}) })
  http.HandleFunc("/metrics", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "course_service_up{service=\"billing\"} 1\n") })
  http.HandleFunc("/invoices", invoices)
  log.Println(`{"level":"info","event":"billing.started"}`)
  log.Fatal(http.ListenAndServe(":8084", nil))
}

func invoices(w http.ResponseWriter, r *http.Request) {
  rows, err := db.Query("SELECT id, order_id, status FROM invoices ORDER BY id")
  if err != nil { http.Error(w, err.Error(), 500); return }
  defer rows.Close(); result := []Invoice{}
  for rows.Next() { var i Invoice; rows.Scan(&i.ID, &i.OrderID, &i.Status); result = append(result, i) }
  json.NewEncoder(w).Encode(result)
}

func consume() {
  url := os.Getenv("RABBITMQ_URL")
  for {
    conn, err := amqp.Dial(url); if err != nil { log.Println("waiting for RabbitMQ", err); time.Sleep(3*time.Second); continue }
    ch, err := conn.Channel(); if err != nil { conn.Close(); time.Sleep(time.Second); continue }
    ch.ExchangeDeclare("order.events", "direct", true, false, false, false, nil)
    q, _ := ch.QueueDeclare("billing.order-created", true, false, false, false, nil)
    ch.QueueBind(q.Name, "order.created", "order.events", false, nil)
    messages, _ := ch.Consume(q.Name, "billing", false, false, false, false, nil)
    for delivery := range messages {
      var event OrderEvent
      if json.Unmarshal(delivery.Body, &event) == nil {
        _, err = db.Exec("INSERT OR IGNORE INTO invoices(order_id,status,created_at) VALUES(?,?,?)", event.OrderID, "CREATED", time.Now().UTC().Format(time.RFC3339))
        if err == nil { log.Printf(`{"level":"info","event":"invoice.created","orderId":%d}`, event.OrderID); delivery.Ack(false); continue }
      }
      delivery.Nack(false, true)
    }
    ch.Close(); conn.Close(); time.Sleep(time.Second)
  }
}
