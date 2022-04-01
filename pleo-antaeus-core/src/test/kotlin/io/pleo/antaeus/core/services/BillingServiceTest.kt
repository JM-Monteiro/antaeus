package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.random.Random

class BillingServiceTest {

    private val cust1 = Customer(100,Currency.EUR)
    private val cust2 = Customer(200,Currency.DKK)

    //CORRECT VALUE
    private val inv1 = Invoice(1,100, Money(BigDecimal.valueOf(40),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NONE.note)
    private val inv11 = Invoice(1,100, Money(BigDecimal.valueOf(40),Currency.EUR),InvoiceStatus.PAID,InvoiceNote.NONE.note)

    //DIFFERENT CURRENCY
    private val inv2 = Invoice(2,100, Money(BigDecimal.valueOf(50),Currency.DKK),InvoiceStatus.PENDING,InvoiceNote.NONE.note)
    private val inv21 = Invoice(2,100, Money(BigDecimal.valueOf(50),Currency.DKK),InvoiceStatus.PENDING,InvoiceNote.DIFFERENTCURRENCY.note)

    //ALREADY PAID
    private val inv3 = Invoice(3,100, Money(BigDecimal.valueOf(50),Currency.EUR),InvoiceStatus.PAID,InvoiceNote.NONE.note)

    //NO USER
    private val inv4 = Invoice(4,404, Money(BigDecimal.valueOf(50),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NONE.note)
    private val inv41 = Invoice(4,404, Money(BigDecimal.valueOf(50),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NOCUSTOMER.note)

    //NO FUNDS
    private val inv5 = Invoice(5,100, Money(BigDecimal.valueOf(5000000),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NONE.note)
    private val inv51 = Invoice(5,100, Money(BigDecimal.valueOf(5000000),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NOFUNDS.note)

    private val pendingList = listOf(inv1,inv2,inv4,inv5)
    private val responseList = listOf(inv11,inv21,inv41,inv51)

    private val customerServiceMock = mockk<CustomerService> {
        every { fetch(404) } throws CustomerNotFoundException(404)
        every { fetch(100) } returns cust1
        every { fetch(200) } returns cust2
    }

    private val invoiceServiceMock = mockk<InvoiceService> {
        every { fetch(404) } throws InvoiceNotFoundException(404)
        every { fetch(1) } returns inv1
        every { fetch(2) } returns inv2
        every { fetch(3) } returns inv3
        every { fetch(4) } returns inv4
        every { fetch(5) } returns inv5
        every { fetchAll(InvoiceStatus.PENDING.toString()) } returns pendingList
        every {paidInvoice(inv1)} returns inv11
        every {failedPaymentInvoice(inv2,InvoiceNote.DIFFERENTCURRENCY)} returns inv21
        every {failedPaymentInvoice(inv4,InvoiceNote.NOCUSTOMER)} returns inv41
        every {failedPaymentInvoice(inv5,InvoiceNote.NOFUNDS)} returns inv51
    }


    private val paymentProviderMock = mockk<PaymentProvider>{
        every {charge(inv1)} returns true
        every {charge(inv2)} returns true
        every {charge(inv3)} returns true
        every {charge(inv4)} returns true
        every {charge(inv5)} returns false
    }


    private val billingService = BillingService(paymentProviderMock,invoiceServiceMock,customerServiceMock)


    @Test
    fun `will succeed`(){
        val returnedInv = billingService.processPendingInvoice(1)
        assert(returnedInv == inv11)
    }

    @Test
    fun `will succeed batch`(){
        val resp = billingService.processAllPendingInvoices()
        assert(resp == responseList)
    }

    @Test
    fun `will fail if different currency`(){
        val returnedInv = billingService.processPendingInvoice(2)
        assert(returnedInv == inv21)
    }

    @Test
    fun `will throw if already paid`(){
        assertThrows<InvoiceAlreadyPaidException> {
            billingService.processPendingInvoice(3)
        }
    }

    @Test
    fun `will fail if no customer`(){
        val returnedInv = billingService.processPendingInvoice(4)
        assert(returnedInv == inv41)
    }

    @Test
    fun `will fail if no funds`(){
        val returnedInv = billingService.processPendingInvoice(5)
        assert(returnedInv == inv51)
    }



}