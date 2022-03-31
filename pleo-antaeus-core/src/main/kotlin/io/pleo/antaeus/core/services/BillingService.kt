package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceNote
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {


    // checks the database for the non-processed invoices and charges the client the necessary amount
    //TODO: cross check dbs for currency and customer
    fun processAllPendingInvoices():List<Invoice>{
        val pendingInvoices = invoiceService.fetchAll(InvoiceStatus.PENDING.toString())

        val resultList = mutableListOf<Invoice>()

        for (invoice in pendingInvoices){

            var note = InvoiceNote.OTHER
            var status = false

            try{
                status = paymentProvider.charge(invoice)
                if(!status){
                    note = InvoiceNote.NOFUNDS
                }
            }catch(e: Exception) {
                note = if (e is NetworkException){
                    InvoiceNote.NETWORKERROR
                }else{
                    InvoiceNote.OTHER
                }
            }finally {
                if(status){
                    resultList.add(invoiceService.paidInvoice(invoice))
                }else{
                    resultList.add(invoiceService.failedPaymentInvoice(invoice,note))
                }
            }
        }


        return resultList
    }



}
