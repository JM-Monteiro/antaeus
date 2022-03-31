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
    private val inv1 = Invoice(1,100, Money(BigDecimal.valueOf(40),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NONE)

    //DIFFERENT CURRENCY
    private val inv2 = Invoice(2,100, Money(BigDecimal.valueOf(50),Currency.DKK),InvoiceStatus.PENDING,InvoiceNote.NONE)

    //ALREADY PAID
    private val inv3 = Invoice(3,100, Money(BigDecimal.valueOf(50),Currency.EUR),InvoiceStatus.PAID,InvoiceNote.NONE)

    //NO USER
    private val inv4 = Invoice(4,404, Money(BigDecimal.valueOf(50),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NONE)

    //NO FUNDS
    private val inv5 = Invoice(5,100, Money(BigDecimal.valueOf(5000000),Currency.EUR),InvoiceStatus.PENDING,InvoiceNote.NONE)

    private val pendingList = listOf(inv1,inv2,inv4,inv5)

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
    fun `will throw if already paid`(){
        assertThrows<InvoiceAlreadyPaidException> {
            billingService.processPendingInvoice(3)
        }
    }

    @Test
    fun `will throw if no funds`(){
        assertThrows<InvoiceAlreadyPaidException> {
            billingService.processPendingInvoice(3)
        }
    }

}