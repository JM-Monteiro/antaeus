package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceNote
import io.pleo.antaeus.models.InvoiceStatus
import mu.KLogger
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val logger: KLogger = KotlinLogging.logger{}
) {

    fun billProcessingTrigger(){
        logger.info {"Processing invoices"}
        val invoices = processAllPendingInvoices()
        logger.info {"Processed "+invoices.size+" invoices"}
        Timer("BillProcessing").cancel()
        Timer("BillProcessing",false).schedule(getTimeTilNextMonth()){
           billProcessingTrigger()
        }
    }

    // checks the database for the non-processed invoices and charges the client the necessary amount
    fun processAllPendingInvoices(): List<Invoice> {
        val pendingInvoices = invoiceService.fetchAll(InvoiceStatus.PENDING.toString())

        val resultList = mutableListOf<Invoice>()

        for (invoice in pendingInvoices) {
            resultList.add(executePayment(invoice))
        }
        return resultList
    }

    fun processPendingInvoice(id:Int):Invoice{
        logger.info { "Processing invoice $id" }
        val invoice = invoiceService.fetch(id)

        if(invoice.status==InvoiceStatus.PAID){
            logger.error { "Invoice $id already paid" }
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
                logger.error { "Invoice "+ invoice.id+" and customer "+ customer.id + "have different currency" }
                note = InvoiceNote.DIFFERENTCURRENCY
            } else {
                status = paymentProvider.charge(invoice)
                if (!status) {
                    logger.error { "Customer "+ customer.id + " has no funds" }
                    note = InvoiceNote.NOFUNDS
                }
            }
        } catch (e: Exception) {
            note = when (e) {
                is NetworkException -> {
                    logger.error { "Network error" }
                    InvoiceNote.NETWORKERROR
                }
                is CustomerNotFoundException -> {
                    logger.error { "No customer with id "+ invoice.customerId + "found" }
                    InvoiceNote.NOCUSTOMER
                }

                //prevent unsupported exceptions
                else -> {
                    logger.error { "An error has occurred" }
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



    fun getTimeTilNextMonth():Long{

        val nowCalendar = Calendar.getInstance()


        val nextMonthCalendar = Calendar.getInstance()

        //IF DECEMBER ADVANCE A YEAR AND RESET MONTH
        if (nowCalendar.get(Calendar.MONTH) == 11){
            nextMonthCalendar.set(Calendar.MONTH,0)
            nextMonthCalendar.set(Calendar.YEAR,nowCalendar.get(Calendar.YEAR)+1)
        }else{
            nextMonthCalendar.set(Calendar.MONTH,nowCalendar.get(Calendar.MONTH)+1)
        }

        nextMonthCalendar.set(Calendar.DAY_OF_MONTH,1)
        nextMonthCalendar.set(Calendar.HOUR_OF_DAY,0)
        nextMonthCalendar.clear(Calendar.MINUTE)
        nextMonthCalendar.clear(Calendar.SECOND)
        nextMonthCalendar.clear(Calendar.MILLISECOND)

        logger.info{"Today is "+nowCalendar.timeInMillis}
        logger.info{"Next Time it will process bills "+nextMonthCalendar.timeInMillis}



        logger.info{"Hours til next trigger "+
                TimeUnit.MILLISECONDS.toMinutes(nextMonthCalendar.timeInMillis-nowCalendar.timeInMillis)}


        return nextMonthCalendar.timeInMillis-nowCalendar.timeInMillis
    }

}
