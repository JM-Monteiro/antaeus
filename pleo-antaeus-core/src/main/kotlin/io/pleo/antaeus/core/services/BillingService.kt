package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {


    // checks the database for the non-processed invoices and charges the client the necessary amount
    fun processInvoices():List<Invoice>{
        return invoiceService.fetchAll("PENDING")
    }


}
