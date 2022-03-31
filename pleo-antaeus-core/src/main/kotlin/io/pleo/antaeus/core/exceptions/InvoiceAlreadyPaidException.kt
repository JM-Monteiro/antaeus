package io.pleo.antaeus.core.exceptions

class InvoiceAlreadyPaidException(id: Int) : Exception("Invoice '$id' was already paid")