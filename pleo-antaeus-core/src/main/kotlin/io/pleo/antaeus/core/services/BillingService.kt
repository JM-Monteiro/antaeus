package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceNote
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {


    // checks the database for the non-processed invoices and charges the client the necessary amount
    //TODO: cross check dbs for currency and customer
    fun processAllPendingInvoices(): List<Invoice> {
        val pendingInvoices = invoiceService.fetchAll(InvoiceStatus.PENDING.toString())

        val resultList = mutableListOf<Invoice>()

        for (invoice in pendingInvoices) {
            resultList.add(executePayment(invoice))
        }
        return resultList
    }

    fun processPendingInvoice(id:Int):Invoice{
        val invoice = invoiceService.fetch(id)

        if(invoice.status==InvoiceStatus.PAID){
            throw InvoiceAlreadyPaidException(invoice.id)
        }else{
            return executePayment(invoice)
        }
    }


    private fun executePayment(invoice: Invoice): Invoice {
        var note = InvoiceNote.NONE
        var status = false

        try {
            val customer = customerService.fetch(invoice.customerId)

            if (customer.currency != invoice.amount.currency) {
                note = InvoiceNote.DIFFERENTCURRENCY
            } else {
                status = paymentProvider.charge(invoice)
                if (!status) {
                    note = InvoiceNote.NOFUNDS
                }
            }
        } catch (e: Exception) {
            note = when (e) {
                is NetworkException -> {
                    InvoiceNote.NETWORKERROR
                }
                is CustomerNotFoundException -> {
                    InvoiceNote.NOCUSTOMER
                }

                //prevent unsupported exceptions
                else -> {
                    InvoiceNote.OTHER
                }
            }
        }

        return if (status) {
            invoiceService.paidInvoice(invoice)
        } else {
            invoiceService.failedPaymentInvoice(invoice, note)
        }

    }
}
