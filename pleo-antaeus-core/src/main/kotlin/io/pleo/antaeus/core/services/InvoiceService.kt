/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.StatusNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceNote
import io.pleo.antaeus.models.InvoiceStatus
import mu.KLogger
import mu.KotlinLogging

class InvoiceService(
    private val dal: AntaeusDal,
    private val logger: KLogger = KotlinLogging.logger{}
    ) {
    fun fetchAll(status:String?): List<Invoice> {

        return if(status.isNullOrBlank()){
            dal.fetchInvoices()
        }else{
            fetchByStatus(status)
        }
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }


    /**
     * Changes an invoice status to "Paid".
     * @param invoice successfully charged invoice.
     * @throws InvoiceNotFoundException
     * @return The invoice after the database update.
     */
    fun paidInvoice(invoice: Invoice):Invoice{
        logger.error { "Invoice %d payment successful".format(invoice.id) }
        return dal.paidInvoice(invoice.id) ?: throw InvoiceNotFoundException(invoice.id)
    }


    /**
     * Adds a note to a pending invoice that failed the charge process.
     * @param invoice The failed invoice
     * @param note The error that has occurred.
     * @throws InvoiceNotFoundException
     * @return the invoice after database update.
     */
    fun failedPaymentInvoice(invoice: Invoice, note:InvoiceNote):Invoice{
        logger.error { "Invoice %d has failed. Reason: %s".format(invoice.id,note.note)}
        return dal.failedPaymentInvoice(invoice.id,note) ?: throw InvoiceNotFoundException(invoice.id)
    }


    /**
     * Fetches all invoice that have a given status.
     * @param status PAID or PENDING.
     * @throws InvoiceNotFoundException
     */
    private fun fetchByStatus(status: String): List<Invoice> {
        val invoiceStatus: InvoiceStatus = try {
            InvoiceStatus.valueOf(status)
        }catch (e:IllegalArgumentException){
            throw StatusNotFoundException(status)
        }

        return dal.fetchInvoicesByStatus(invoiceStatus)
    }


}
