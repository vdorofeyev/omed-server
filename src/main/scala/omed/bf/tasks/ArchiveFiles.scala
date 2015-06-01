package omed.bf.tasks

import omed.bf.ProcessTask


class ArchiveFiles (
                     val varData: String,
                     val folderName: String,
                     val zipFileName: String,
                     val files: Seq[ArchiveFileAttributes]
                     ) extends ProcessTask("_Meta_BFSArchiveFiles")
{

  def fullPathExp = {
    "@"+folderName + " + @" + zipFileName
  }
}
object ArchiveFiles {
  def apply(xml: scala.xml.Node, varData:String): ArchiveFiles = {
    // определяем имена папки и файла
    val folderName = xml.attribute("Destination").map(_.text.replaceFirst("\\@", "")).orNull
    val zipFileName =   xml.attribute("ArchiveFileName").map(_.text.replaceFirst("\\@", "")).orNull
    // val xml2 =  <_Meta_BFSArchiveFiles _ChangeDate="2013-06-14T16:38:27.747" _CreateDate="2013-06-14T08:18:04.030" StepNumber="50" Destination="@folder" Name="архивирование" _Name="архивирование" BusinessFunctionID="A122339C-447B-4670-8CFC-41ECE3D75748" ArchiveFileName="@fileName" ID="075F4B70-7B59-4D00-AD56-4044BC00A4AF"><Params><_Meta_ArchiveFile _ChangeDate="2013-06-17T12:02:37.070" _CreateDate="2013-06-14T08:18:32.243" DecodeFromBase64="N" Source="@firstFileData" FileName="test1.xml" BFSArchiveFilesID="075F4B70-7B59-4D00-AD56-4044BC00A4AF" ID="D089C8EB-6FE2-43CC-A712-F9E39CE6060F"></_Meta_ArchiveFile></Params></_Meta_BFSArchiveFiles>
    // список файлов для архивирования

    val fileNodes = xml \\ "Params" \\ "_Meta_ArchiveFile"
    val files = fileNodes.map(node => {
      val filename = node.attribute("FileName").map(_.text.replaceFirst("\\@", "")).orNull
      val source = node.attribute("Source").map(_.text.replaceFirst("\\@", "")).orNull
      val isBase64Encoded =node.attribute("IsEncodedInBase64").isDefined && node.attribute("IsEncodedInBase64").get.text=="Y"
      new ArchiveFileAttributes(filename,source,isBase64Encoded)
    })

    new ArchiveFiles(varData, folderName, zipFileName, files)
  }


}
class ArchiveFileAttributes (
                              val  fileName: String,
                              val source: String,
                              val isBase64Encoded : Boolean
                              )
