package io.pleo.antaeus.models

enum class InvoiceNote(val note:String) {
    NOFUNDS("Could not charge the necessary amount."),
    NETWORKERROR("Communication error with provider."),
    OTHER("An error has occurred."),
    DIFFERENTCURRENCY("Invoice's currency is different from the customer's currency."),
    NOCUSTOMER("No customer associated with this invoice."),
    NONE("");

    companion object {
    fun getEnumByString(stringValue:String):InvoiceNote{
        for (value in values()){
            if (stringValue == value.toString()){
                return value
            }
        }
        return NONE
    }
    }
}


