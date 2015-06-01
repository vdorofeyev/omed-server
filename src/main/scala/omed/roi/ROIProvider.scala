package omed.roi
import java.util.Date

/**
 * Протокол для работы с РОИ
 */
trait ROIProvider {
   def exportExpertisesToRoi(begin:String,end:String):String
   def importExpertisesFromRoi(jsonString:String):Boolean
   def loadExpertiseFromROI(old:Boolean)
   def sentExpertiseToROI
}
