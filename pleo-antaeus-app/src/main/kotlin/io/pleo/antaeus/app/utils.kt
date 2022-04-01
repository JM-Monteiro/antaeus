
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import mu.KotlinLogging
import java.math.BigDecimal
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}


fun getTimeTilNextMonth():Long{
    val logger = KotlinLogging.logger{}
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
    logger.info{"Next Time it will process bills"+nextMonthCalendar.timeInMillis}



    logger.info{"Hours til next trigger"+
            TimeUnit.MILLISECONDS.toMinutes(nextMonthCalendar.timeInMillis-nowCalendar.timeInMillis)}


    return nextMonthCalendar.timeInMillis-nowCalendar.timeInMillis
}
