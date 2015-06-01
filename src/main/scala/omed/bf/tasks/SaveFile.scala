package omed.bf.tasks


import omed.bf.{ClientTask, ProcessTask}

class SaveFile (
  override val xml: scala.xml.Node
) extends ProcessTask("_Meta_BFSSaveFile") with ClientTask

object SaveFile {

    def apply(xml: scala.xml.Node): SaveFile = {
      new SaveFile(xml)
    }
    def apply(DestinationFile: String, SourceData: String, IsEncoded: String): SaveFile = {
      new SaveFile(<_Meta_BFSSaveFile
        FilePath={ DestinationFile }
        Source={ SourceData }
         IsEncodedInBase64= {IsEncoded}/>)
    }

}