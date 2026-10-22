using System.Text;
using System.Text.Json;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
using StackExchange.Redis;

var builder = WebApplication.CreateBuilder(args);
var redis = await ConnectionMultiplexer.ConnectAsync(Environment.GetEnvironmentVariable("REDIS_CONNECTION") ?? "localhost:6379");
builder.Services.AddSingleton<IConnectionMultiplexer>(redis);
builder.Services.AddHostedService<OrderNotificationConsumer>();
var app = builder.Build();
app.MapGet("/health", () => Results.Ok(new { status = "UP", service = "notification-service" }));
app.MapGet("/metrics", () => Results.Text("course_service_up{service=\"notification\"} 1\n"));
app.MapGet("/notifications", async (IConnectionMultiplexer connection) => {
    var values = await connection.GetDatabase().ListRangeAsync("notifications");
    return Results.Ok(values.Select(v => JsonSerializer.Deserialize<object>((string?)v ?? "null")).ToArray());
});
app.Logger.LogInformation("notification-service started");
app.Run("http://0.0.0.0:8085");

sealed class OrderNotificationConsumer : BackgroundService {
    private readonly IConnectionMultiplexer redis;
    private readonly ILogger<OrderNotificationConsumer> logger;
    public OrderNotificationConsumer(IConnectionMultiplexer redis, ILogger<OrderNotificationConsumer> logger) { this.redis = redis; this.logger = logger; }
    protected override async Task ExecuteAsync(CancellationToken token) {
        while (!token.IsCancellationRequested) {
            try {
                var factory = new ConnectionFactory { HostName = Environment.GetEnvironmentVariable("RABBITMQ_HOST") ?? "localhost", UserName = "course", Password = "course", DispatchConsumersAsync = true };
                using var connection = factory.CreateConnection(); using var channel = connection.CreateModel();
                channel.ExchangeDeclare("order.events", ExchangeType.Direct, durable: true);
                channel.QueueDeclare("notification.order-created", durable: true, exclusive: false, autoDelete: false);
                channel.QueueBind("notification.order-created", "order.events", "order.created");
                var consumer = new AsyncEventingBasicConsumer(channel);
                consumer.Received += async (_, message) => {
                    var notification = JsonSerializer.Serialize(new { type = "ORDER_CONFIRMED", receivedAt = DateTime.UtcNow, payload = JsonDocument.Parse(message.Body.ToArray()).RootElement });
                    await redis.GetDatabase().ListRightPushAsync("notifications", notification);
                    channel.BasicAck(message.DeliveryTag, false);
                    logger.LogInformation("notification.saved");
                };
                channel.BasicConsume("notification.order-created", false, consumer);
                while (!token.IsCancellationRequested) await Task.Delay(1000, token);
            } catch (Exception error) { logger.LogWarning(error, "Waiting for RabbitMQ"); await Task.Delay(3000, token); }
        }
    }
}
