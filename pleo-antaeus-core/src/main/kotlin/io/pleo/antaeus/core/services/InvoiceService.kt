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

    fun paidInvoice(invoice: Invoice):Invoice{
        logger.error { "Invoice "+invoice.id +" payment successful" }
        return dal.paidInvoice(invoice.id) ?: throw InvoiceNotFoundException(invoice.id)
    }

    fun failedPaymentInvoice(invoice: Invoice, note:InvoiceNote):Invoice{
        logger.error { "Invoice "+invoice.id +": "+ note.note }
        return dal.failedPaymentInvoice(invoice.id,note) ?: throw InvoiceNotFoundException(invoice.id)
    }

    private fun fetchByStatus(status: String): List<Invoice> {
        val invoiceStatus: InvoiceStatus = try {
            InvoiceStatus.valueOf(status)
        }catch (e:IllegalArgumentException){
            throw StatusNotFoundException(status)
        }

        return dal.fetchInvoicesByStatus(invoiceStatus)
    }


}
