package omed.rest.parsers

import omed.fer.HL7PatientRecordMessage

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 21.10.13
 * Time: 16:22
 * To change this template use File | Settings | File Templates.
 */
object HL7Parser {
    def parseFERMessage(message:String):HL7PatientRecordMessage = {
        HL7PatientRecordMessage(message)
    }
}
