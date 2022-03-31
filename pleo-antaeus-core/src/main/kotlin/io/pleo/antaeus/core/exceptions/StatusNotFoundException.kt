package io.pleo.antaeus.core.exceptions

class StatusNotFoundException(status:String) : Exception("$status is not a Invoice Status")
