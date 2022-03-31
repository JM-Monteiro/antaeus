package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {


    // checks the database for the non-processed invoices and charges the client the necessary amount
    //TODO: cross check dbs for currency and customer
    fun processAllPendingInvoices():List<Invoice>{
        val pendingInvoices = invoiceService.fetchAll(InvoiceStatus.PENDING.toString())

        for (invoice in pendingInvoices){

            val note:String

            val status: Boolean = try{
                paymentProvider.charge(invoice)
            }catch(e: Exception) {
                note = e.toString()
                false
            }

            if(status){
                //TODO:change to paid
            }else{
                //TODO:publish a note and keep pending
            }

        }
        return emptyList()
    }



}
