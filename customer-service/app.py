import os
import logging
from fastapi import FastAPI, HTTPException
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel, EmailStr
from pymongo import MongoClient
from bson import ObjectId

logging.basicConfig(level=logging.INFO, format='%(message)s')
app = FastAPI(title='Customer Service')
collection = MongoClient(os.getenv('MONGO_URL', 'mongodb://localhost:27017')).customers.customers

class CustomerInput(BaseModel):
    name: str
    email: EmailStr
    phone: str | None = None

def serialise(customer):
    customer['id'] = str(customer.pop('_id'))
    return customer

@app.get('/health')
def health(): return {'status': 'UP', 'service': 'customer-service'}

@app.get('/metrics', response_class=PlainTextResponse)
def metrics(): return 'course_service_up{service="customer"} 1\n'

@app.post('/customers', status_code=201)
def create_customer(customer: CustomerInput):
    identifier = collection.insert_one(customer.model_dump()).inserted_id
    logging.info('{"level":"info","event":"customer.created"}')
    return serialise(collection.find_one({'_id': identifier}))

@app.get('/customers/{customer_id}')
def get_customer(customer_id: str):
    try: customer = collection.find_one({'_id': ObjectId(customer_id)})
    except Exception: customer = None
    if not customer: raise HTTPException(404, 'Customer not found')
    return serialise(customer)
