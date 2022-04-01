package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceNote
import io.pleo.antaeus.models.InvoiceStatus
import mu.KLogger
import mu.KotlinLogging
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val logger: KLogger = KotlinLogging.logger{}
) {


    /**
     * Processes the bills in the first day of each month.
     * @return The list of processed invoices.
     */
    fun billProcessingTrigger(): List<Invoice> {
        logger.info {"Processing invoices"}
        val invoices = processAllPendingInvoices()
        logger.info {"Processed %d invoices".format(invoices.size)}
        Timer("BillProcessing").cancel()
        Timer("BillProcessing",false).schedule(getTimeTilNextMonth()){
           billProcessingTrigger()
        }
        return invoices
    }

    /**
     * Repeats the billing process for a failed Invoice due to network error
     * If the same network error occurs, a timer is launched that triggers after an hour.
     * This process is only triggered 3 times.
     * @param failInv The invoice that failed.
     * @param repeats The current number of retries.
     */
    private fun retryNetworkError(failInv: Invoice, repeats: Int){
        val invoice = processPendingInvoice(failInv.id)
        if(repeats+1 == 3 || invoice.status == InvoiceStatus.PAID){
            Timer("NetworkError"+invoice.id).cancel()
        }else{
            Timer("NetworkError"+invoice.id).cancel()
            val backoff = Duration.ofHours(1).toMillis()
            Timer("NetworkError"+invoice.id,false).schedule(backoff){
                retryNetworkError(invoice,repeats+1)
            }
        }


    }



    /**
     * Batch processes all invoices that have the Pending status in the database and tries to charge the
     * customers the necessary amount.
     * @returns The list of invoices after processing.
     */
    fun processAllPendingInvoices(): List<Invoice> {
        val pendingInvoices = invoiceService.fetchAll(InvoiceStatus.PENDING.toString())

        val resultList = mutableListOf<Invoice>()

        for (invoice in pendingInvoices) {
            resultList.add(executePayment(invoice))
        }
        return resultList
    }

    /**
     * Processes an invoice with a given id and tries to charge the
     * customer the necessary amount.
     * @throws InvoiceAlreadyPaidException if invoice was already paid.
     * @param id The invoice's id.
     * @returns The invoice after processing.
     */
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


    /**
     * Executes the payment. Checks if customer exists and the currency type match.
     * If any error occurs during the payment, an appropriate note is added to the invoice.
     * In the case of the NetworkException, the function prepares the invoice to be processed at a later date with a
     * maximum retries of 3.
     * @param invoice The invoice that will be processed.
     * @returns The invoice after processing.
     */
    private fun executePayment(invoice: Invoice): Invoice {
        var note = InvoiceNote.NONE
        var status = false

        try {
            val customer = customerService.fetch(invoice.customerId)

            if (customer.currency != invoice.amount.currency) {
                logger.error { "Invoice %d and customer %d have different currency".format(invoice.id,customer.id)}
                note = InvoiceNote.DIFFERENTCURRENCY
            } else {
                status = paymentProvider.charge(invoice)
                if (!status) {
                    logger.error { "Customer %d has no funds".format(customer.id) }
                    note = InvoiceNote.NOFUNDS
                }
            }
        } catch (e: Exception) { // an error as occurred
            note = when (e) {
                is NetworkException -> {

                    //retries the billing process 3 times
                    val backoff = Duration.ofHours(1).toMillis()
                    logger.error {"Network error trying again in 1 hour"}
                    //sets a timer for this specific invoice
                    Timer("NetworkError"+invoice.id,false).schedule(backoff){
                        retryNetworkError(invoice,0)
                    }

                    InvoiceNote.NETWORKERROR
                }
                is CustomerNotFoundException -> {
                    logger.error { "No customer with id %d was found.".format(invoice.customerId)}
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


    /**
     * Calculates the time in milliseconds until the next month's start.
     * @returns The time in milliseconds
     */
    fun getTimeTilNextMonth():Long{

        val nowCalendar = Calendar.getInstance()


        val nextMonthCalendar = Calendar.getInstance()

        //If it is December, advance a year and resets month.
        if (nowCalendar.get(Calendar.MONTH) == Calendar.DECEMBER){
            nextMonthCalendar.set(Calendar.MONTH,Calendar.JANUARY)
            nextMonthCalendar.set(Calendar.YEAR,nowCalendar.get(Calendar.YEAR)+1)
        }else{
            nextMonthCalendar.set(Calendar.MONTH,nowCalendar.get(Calendar.MONTH)+1)
        }

        nextMonthCalendar.set(Calendar.DAY_OF_MONTH,1)
        nextMonthCalendar.set(Calendar.HOUR_OF_DAY,0)
        nextMonthCalendar.clear(Calendar.MINUTE)
        nextMonthCalendar.clear(Calendar.SECOND)
        nextMonthCalendar.clear(Calendar.MILLISECOND)

        val today = Date.from(nowCalendar.toInstant())
        logger.info{ "Today is $today" }

        val nextTime = Date.from(nextMonthCalendar.toInstant())
        logger.info{"Will process again at: $nextTime"}



        return nextMonthCalendar.timeInMillis-nowCalendar.timeInMillis
    }



    /*
    test function
    fun getTimeTilNextMinute():Long{

        val nowCalendar = Calendar.getInstance()
        val nextMonthCalendar = Calendar.getInstance()

        nextMonthCalendar.set(Calendar.SECOND,nowCalendar.get(Calendar.SECOND)+15)

        val today = Date.from(nowCalendar.toInstant());
        logger.info{ "Today is $today" }

        val nextTime = Date.from(nextMonthCalendar.toInstant());
        logger.info{"Will process again at: $nextTime"}

        return nextMonthCalendar.timeInMillis-nowCalendar.timeInMillis
    }*/

}
