package omed.bf.handlers

import com.google.inject.Inject
import omed.bf._
import omed.model.{SimpleValue, EntityInstance, Value}
import omed.model.services.ExpressionEvaluator
import omed.data.{DataAwareConfiguration, DataWriterService}
import omed.bf.tasks.ArchiveFiles
import omed.lang.eval.Configuration

import omed.system.ContextProvider

class ArchiveFilesHandler extends ProcessStepHandler {

  @Inject
  var contextProvider: ContextProvider = null

  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  override val name = "_Meta_BFSArchiveFiles"


//  def canHandle(step: ProcessTask) = {
//    step.stepType == name
//  }

  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[ArchiveFiles]
    val destinationVar = targetTask.varData.replaceFirst("\\@", "")
    val zipData = SimpleValue( zip("tmp",targetTask.files,context) )
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг Архивирование",context,Map("zipData"->zipData)))
    Map(destinationVar->zipData)
  }
  def zip(out: String, files: Seq[omed.bf.tasks.ArchiveFileAttributes],context: Map[String, Value]) = {
    import java.io.{ ByteArrayOutputStream, FileInputStream, FileOutputStream }
    import java.util.zip.{ ZipEntry, ZipOutputStream }
    import net.iharder.Base64
    val byteArray = new ByteArrayOutputStream()
    import java.nio.charset.Charset
    val zip = new ZipOutputStream(byteArray) //,Charset.forName("UTF-8")

    files.foreach { fileAttribute =>
      val SourceData = context.get(fileAttribute.source)
      val fileName = context.get(fileAttribute.fileName)
      if(SourceData.isDefined && fileName.isDefined){
        val textData =SourceData.get.toString()
        val zipEntry = new ZipEntry(fileName.get.toString())

        zip.putNextEntry(zipEntry)

        val bytes = if(fileAttribute.isBase64Encoded) Base64.decode(textData) else  textData.getBytes("UTF-8")

        zip.write(bytes)
        zip.closeEntry()
      }
    }
    zip.close()

    Base64.encodeBytes(byteArray.toByteArray())
  }

}