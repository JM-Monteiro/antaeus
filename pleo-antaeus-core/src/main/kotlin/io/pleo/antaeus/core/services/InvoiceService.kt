/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.StatusNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
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
        return dal.paidInvoice(invoice.id) ?: throw InvoiceNotFoundException(invoice.id)
    }

    fun failedPaymentInvoice(invoice: Invoice):Invoice{
        return dal.failedPaymentInvoice(invoice.id,invoice.note) ?: throw InvoiceNotFoundException(invoice.id)
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
