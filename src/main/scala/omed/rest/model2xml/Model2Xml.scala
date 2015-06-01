package omed.rest.model2xml

import net.iharder.Base64
import scala.xml._
import omed.model._
import ru.atmed.omed.beans.model.meta._
import omed.data.{DataViewRow, DataViewTable}
import omed.cache.ExecStatProvider

/**
 * Служебный класс для формирования XML по объектному представлению
 * и для получения объектной структуры по XML в процессе разбора запросов
 * от клиентских приложений.
 */
class Model2Xml() {

  /**
   * Теги для которых значение null, добавляются
   *
   * Например:
   * value = 123, тег "speed"
   * value = 300, тег "distance"
   * Будет: <speed>123</speed><distance>300</distance>
   *
   * value = 123, тег "speed"
   * value = null, тег "distance"
   * Будет: <speed>123</speed>
   *
   */
  private def tag(tag: String, obj: Object): String = {
    if (obj == null) return ""

    var result = new StringBuilder()
    result.append("<").append(tag).append(">")
      .append(obj.toString())
      .append("</").append(tag).append(">")

    result.toString()
  }

  def clean(inp: String) =
    if (inp != null)
      inp
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    else null


  /**
   *
   * Главное меню
   *
   * Формат:
   *
   * <?xml version="1.0" encoding="UTF-8" ?>
   * <menuElements>
   *   <menu>
   *     <id>[ИдентификаторЭлементаМеню]</id>
   *     <name>[НаименованиеНаФормеИлиГриде]</name>
   *     <parentMenuId>[ИдентификаторРодительскогоЭлемента]</parentMenuId>
   *     <openViewId>[ИдентификаторФормы]</openViewId>
   *     <glyph>[ИконкаЭлементаМеню]</glyph>
   *   </menu>
   *   ...
   *   <menu>...</menu>
   * </menuElements>
   */
  def mainMenuToXml(menus: Seq[AppMenu]): String = {
    if (menus != null) {
      var xml = new StringBuilder()
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")

      // сформировать список элементов меню 
      // <menu>...</menu><menu>...</menu>....<menu>...</menu>
      var menuList = new StringBuilder()
      for (menu <- menus) {
        menuList.append(this.tag("menuItem",
          new StringBuilder().append(this.tag("id", menu.id))
            .append(this.tag("name", menu.name))
            .append(this.tag("parentMenuId", menu.parentId))
            .append(this.tag("businessFunctionId", menu.businessFunctionId))
            .append(this.tag("openViewId", menu.openViewId))
            .append(this.tag("glyph", {
              if (menu.glyph != null)
                Base64.encodeBytes(menu.glyph)
              else
                null
            }))))
      }

      //cформировать окончательную структуру
      xml.append(this.tag("mainMenu", menuList))

      return xml.toString()
    }
    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><mainMenu></mainMenu>"
  }

  /**
   *
   * Меню приложения
   *
   * <?xml version="1.0" encoding="UTF-8" ?>
   * <menu>
   *   <menuItem>
   *     <id>[ИдентификаторЭлементаМеню]</id>
   *     <name>[НаименованиеЭлементаМеню]</name>
   *     <parentMenuID>[ИдентификаторРодительскогоЭлемента]</parentMenuID>
   *     <glyph>[Иконка]</glyph>
   *     <methodTypeCharCode>[ТипМетода]</methodTypeCharCode>
   *     <methodCode>[Метода]</methodCode>
   *     <isConfirmation>[ФлагПодтверждения]</isConfirmation>
   *     <isRefresh>[ФлагОбновленияФормы]</isRefresh>
   *     <msg>[Сообщение]</msg>
   *     <shortcut>[СочетаниеКлавишДляЭлементаМеню]</shortcut>
   *   </menuItem>
   *   ...
   *   <menuItem>...</menuItem>
   * </menu>
   */
  def contextMenuToXml(menus: Seq[ContextMenu]): String = {
    if (menus != null) {
      var xml = new StringBuilder()
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")

      // сформировать список элементов меню 
      // <menuItem>...</menuItem><menuItem>...</menuItem>....<menuItem>...</menuItem>
//      var menuList = new StringBuilder()
//      for (menu <- menus)
//        menuList.append(this.tag("menuItem", this.contextMenuItemToXml(menu)))

      //cформировать окончательную структуру
      xml.append(this.tag("menu", menus.map(f =>f.xmlString).mkString("\n")).toString())

      return xml.toString()
    }
    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><menu></menu>"
  }

  /**
   * Метаописание формы-грида
   */
  def metaGridToXml(metaFormGrid: MetaFormGrid): String = {
    if (metaFormGrid != null) {
      var xml = new StringBuilder()
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")
      xml.append(metaFormGrid.xmlString)
      xml.toString
    } else
      throw new Exception("Parameter \"metaGrid\" is null, should be not null.")
  }

  /**
   * Метаописание формы-карточки
   */
  def metaCardToXml(metaCard: MetaCard): String = {
    if (metaCard != null) {
      var xml = new StringBuilder()
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")

      val contextMenuItems =
        if (metaCard.contextMenu == null)  null
         else  metaCard.contextMenu.map(f=>f.xmlString).mkString("\n")


      val fieldGroups =
        if (metaCard.groups != null)
          metaCard.groups.foldLeft(new StringBuilder())(
            (sb, el) =>
              sb.append(this.tag("groupItem", new StringBuilder()
                .append(this.tag("id", el.getId))
                .append(this.tag("caption", el.getCaption))
                .append(this.tag("sortOrder", el.getSortOrder.toString)))))
        else null

      val cardsInCard =
        if (metaCard.cardsInCard!=null)
          metaCard.cardsInCard.foldLeft(new StringBuilder())(
            (sb, el) =>
              sb.append(this.tag("objectInCardItem", new StringBuilder()
                .append(this.tag("id", el.id))
                .append(this.tag("groupId",el.groupId))
                .append(this.tag("caption", el.caption))
                .append(this.tag("gridId",el.gridId))
                .append(this.tag("objectViewCardId",el.insertViewCardId))
                .append(this.tag("sortOrder", el.sortOrder.toString)))))
        else null

      val gridsInCard = metaCard.gridsInCard.map(f =>f.xmlString).mkString("\n")

      val fieldSections =
        if (metaCard.sections != null)
          metaCard.sections.foldLeft(new StringBuilder())(
            (sb, el) =>
              sb.append(this.tag("sectionItem", new StringBuilder()
                .append(this.tag("id", el.getId))
                .append(this.tag("caption", el.getCaption))
                .append(this.tag("groupId", el.getGroupId))
                .append(this.tag("sortOrder", el.getSortOrder.toString))
                .append(this.tag("sectionParentId", el.sectionParentId)))))
        else null
      val fieldList =
        if (metaCard.fields != null)
          metaCard.fields.foldLeft(new StringBuilder())(
            (sb, field) => {
              sb.append(this.tag("field", new StringBuilder()
                .append(this.tag("id", field.getViewFieldId))
                .append(this.tag("codeName", field.getCodeName))
                .append(this.tag("caption", field.getCaption))
                .append(this.tag("sortOrder", field.getSortOrder.toString))
                .append(this.tag("isReadOnly", field.getIsReadOnly.toString))
                .append(this.tag("editorType", field.getEditorType))
                .append(this.tag("format", field.getFormat))
                .append(this.tag("isDropDownNotAllowed", field.getIsDropDownNotAllowed.toString))
                .append(this.tag("isMasked", field.getIsMasked.toString))
                .append(this.tag("isVisible", field.getIsVisible.toString))
                .append(this.tag("defaultFormGridId", field.getDefaultFormGridId))
                .append(this.tag("typeCode", field.getTypeCode))
                .append(this.tag("typeExtInfo", field.getTypeExtInfo))
                .append(this.tag("height", field.getHeight.toString))
                .append(this.tag("extInfo", field.getExtInfo))
                .append(this.tag("width", field.getWidth.toString))
                .append(this.tag("groupId", field.getGroupId))
                .append(this.tag("sectionId",field.getSectionId))
                .append(this.tag("isRequired", field.getIsRequired.toString))
                .append(this.tag("refParams", field.refParams))
                .append(this.tag("mask", field.mask))
                .append(this.tag("isJoinMask", field.isJoinMask.toString))
                .append(this.tag("captionStyle", field.captionStyle))
                .append(this.tag("isJoinPrev", field.isJoined.toString))
                .append(this.tag("isRefreshOnChange", field.isRefreshOnChange.toString))))
            })
        else null

      val refGrids =
        if (metaCard.refGrids != null)
          metaCard.refGrids.foldLeft(new StringBuilder())(
            (sb, el) => sb.append(this.tag("grid", el.xmlString).toString))
        else null

      xml.append(this.tag("cardForm",
        new StringBuilder().append(this.tag("classId", metaCard.getClassId))
          .append(this.tag("viewCardId", metaCard.viewCardId))
          .append(this.tag("caption", metaCard.caption))
          .append(this.tag("glyph", {
            if (metaCard.glyph != null)
              Base64.encodeBytes(metaCard.glyph)
            else
              null
          }))
          .append(this.tag("width", metaCard.width.toString))
          .append(this.tag("height", metaCard.height.toString))
          .append(this.tag("fieldsPanelHeight", metaCard.fieldsPanelHeight.toString))
          .append(this.tag("isReadOnly", metaCard.isReadOnly.toString))
          .append(this.tag("isVisibleAlias", metaCard.isVisibleAlias.toString))
          .append(this.tag("contextMenu", contextMenuItems))
          .append(this.tag("fields", fieldList))
          .append(this.tag("groups", fieldGroups))
          .append(this.tag("sections",fieldSections))
          .append(this.tag("objectsInCard",cardsInCard))
          .append(this.tag("gridsInCard",gridsInCard))
          .append(this.tag("refGrids", refGrids))))

      xml.toString
    } else
      throw new Exception("Parameter \"metaCard\" is null, should be not null.")
  }

  def treeFilterToXml(treeFilter: Seq[FilterNode]): String = {
    if (treeFilter != null) {
      var xml = new StringBuilder()
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")

      /**Сформировать параметры */
      def nodeData(nodeData: List[NodeParameter]) =
        if (nodeData.count(_ => true) > 0)
          nodeData.foldLeft(new StringBuilder())(
            (sb, el) =>
              sb.append(this.tag("parameter", new StringBuilder()
                .append(this.tag("id", el.id))
                .append(this.tag("varName", el.varName))
                .append(this.tag("editorType", el.editorType))
                .append(this.tag("caption", el.caption))
                .append(this.tag("defaultValue", el.defaultValue))
                .append(this.tag("referenceParams", el.referenceParams))
                .append(this.tag("parameterSqlFilter", el.parameterSqlFilter))
                .append(this.tag("viewFieldId", el.viewFieldId))
                .append(this.tag("typeTypeCode", el.typeTypeCode))
                .append(this.tag("typeExtInfo", el.typeExtInfo))
                .append(this.tag("isNoTerminalSelection", el.isNoTerminalSelection.toString))
                .append(this.tag("isMultiSelect", el.isMultiSelect.toString)))))

        else null

      val nodeList =
        if (treeFilter.count(_ => true) > 0)
          treeFilter.foldLeft(new StringBuilder())(
            (sb, el) =>
              sb.append(this.tag("node", new StringBuilder()
                .append(this.tag("nodeId", el.id))
                .append(this.tag("name", el.name))
                .append(this.tag("parentNodeId", el.parentId))
                .append(this.tag("nodeData", nodeData(el.data))))))
        else ""

      xml.append(this.tag("nodes", nodeList))

      xml.toString
    } else
      throw new Exception("Parameter \"treeFilter\" is null, should be not null.")
  }

  /**
   * Получить данные из запроса на выполнение действий
   *
   * Формат:
   *
   * <params>
   *   <classId>[идентификатор_класса]</classId>
   *   <method>[метод]</method>
   *   <recordId>[Идентификатор объекта класса, для которого выполняется метод]</recordId>
   * </params>
   */
  def parseExecuteActionReq(xml_str: String): (String, String, String) = {

    val xml = XML.loadString(xml_str)
    val classId = (xml \\ "classId")(0).text
    val method = (xml \\ "method")(0).text
    val recordId = try {
      (xml \\ "recordId")(0).text
    } catch {
      case _ => null
    }

    (classId, method, recordId)
  }

  /**
   * Сформировать xml для ответа пользователя
   *
   * Формат:
   *
   * <result>
   *   <returnCode>[формальный признак результата]</returnCode>
   *   <message>[сообщение_пользователю]</message>
   * </result>
   */
  def standardAnswerToXml(returnCode: Int, msg: String): String =
    this.standardAnswerToXml(returnCode, msg, null)

  def standardAnswerToXml(returnCode: Int, msg: String,
    warns: Seq[CompiledValidationRule],execStatProvider:ExecStatProvider = null): String = {
    val xml = new StringBuilder()
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")

    xml.append(this.tag("result",
      new StringBuilder().append(this.tag("returnCode", returnCode.toString))
        .append(this.tag("message", msg))
        .append(this.tag("validation", this.validationRulesToXml(warns)))
        .append(if(execStatProvider!=null) execStatProvider.toXml else "")))

    xml.toString()
  }

  /**
   * Сформировать xml по результатам валидации
   */
  def validationRulesToXml(validations: Seq[CompiledValidationRule]): String = {

    // добавить результаты валидации
    val validation: String = if (validations == null || validations.isEmpty)
      null
    else {
      val classValidations = validations.filter(_.validationRule.isInstanceOf[ClassValidationRule])

      val records = if (classValidations.isEmpty)
          ""
      else
        <record>
          { classValidations.map(validation =>
          <result>
            <code>{ validation.validationRule.asInstanceOf[ClassValidationRule]
              .validationResultType match {
                case ValidationResultType.Error => "error"
                case ValidationResultType.Warning => "warning"
              }
            }</code>
            <message>{ validation.validationRule.falseMessage }</message>
            { if (!validation.compiled.condition.getUsedVariableFields("this").isEmpty)
              <fields>
                { validation.compiled.condition.getUsedVariableFields("this").map(
                  fieldName => <field>{ fieldName }</field> )}
              </fields>
            }
          </result>
          ) }
        </record>

      val fieldValidations = validations.filter(_.validationRule.isInstanceOf[FieldValidationRule])

      val fields = if (fieldValidations.isEmpty)
        ""
      else
        <fields>
          { fieldValidations.map(err =>
          <result>
            <fieldId>{ err.asInstanceOf[FieldValidationRule].propertyId }</fieldId>
            <message>{ err.asInstanceOf[FieldValidationRule].falseMessage }</message>
          </result>
        )}
        </fields>

      new StringBuilder()
        .append(this.tag("recordId", null))
        .append(records)
        .append(fields)
        .toString
    }
    validation
  }

  def validationErrorsToXml(errors: Seq[CompiledValidationRule]): String = {
    val xml = new StringBuilder()
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")

    val exceptWarnings = errors.filter(_.validationRule match {
      case cv: ClassValidationRule => cv.validationResultType != ValidationResultType.Warning
      case _ => true
    })

    val validation = this.validationRulesToXml(exceptWarnings)

    val returnCode = -5
    val msg = "Ошибка валидации."

    xml.append(this.tag("result",
      new StringBuilder()
        .append(this.tag("returnCode", returnCode.toString()))
        .append(this.tag("message", msg))
        .append(this.tag("validation", validation))))

    xml.toString()
  }

  /**
   * Сформировать xml для коллекции метаклассов
   */
  def metaClassesToXml(metaClasses: Seq[MetaObject]) = {
    val xml = new StringBuilder()
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")

    val classesXml = metaClasses.foldLeft(new StringBuilder())((sb, clazz) => {
      val fx = clazz.fields.foldLeft(new StringBuilder)((sb, field) =>
        sb.append(this.tag("field",
          new StringBuilder()
            .append(this.tag("id", field.id))
            .append(this.tag("code", field.code))
            .append(this.tag("name", field.code))
            .append(this.tag("metaType", {
              if (field.isInstanceOf[DataField])
                (field.asInstanceOf[DataField]).dataType.toString
              else if (field.isInstanceOf[ReferenceField] && (field.asInstanceOf[ReferenceField]).refObjectCode != null)
                (field.asInstanceOf[ReferenceField]).refObjectCode
              else if (field.isInstanceOf[BackReferenceField] && (field.asInstanceOf[BackReferenceField]).refObject != null)
                (field.asInstanceOf[BackReferenceField]).refObject.code
              else
                null
            }))
            .append(this.tag("type", {
              if (field.isInstanceOf[DataField])
                "value"
              else if (field.isInstanceOf[ReferenceField])
                "ref"
              else if (field.isInstanceOf[BackReferenceField])
                "backref"
              else
                null
            })))))

      sb.append(this.tag("class", new StringBuilder()
        .append(this.tag("id", clazz.id))
        .append(this.tag("code", clazz.code))
        .append(this.tag("name", clazz.code))
        .append(this.tag("fields", fx))))
    })

    xml.append(this.tag("classes", classesXml))

    xml.toString()
  }

  def convert(rs: DataViewTable, hiddenFields: Set[String] = Set(),
              refGridSettings: Seq[StatusWindowGrid] = null,execStatProvider:ExecStatProvider = null,sectionSettings :Seq[StatusSection] = null): String = {
    // format pair as xml node
    val resultsb = new StringBuilder("<result>").append(convertDataTable(rs,hiddenFields))

    if (refGridSettings != null) {
      val grids =
        <refGrids>
          { refGridSettings.map(g =>
          <grid id={ g.windowGridId }
                isVisible={ if (g.isVisible) "true" else "false" }
                isDeleteAllowed={ if (g.isDeleteAllowed) "true" else "false" }
                isInsertAllowed={ if (g.isInsertAllowed) "true" else "false" }
                isEditAllowed={ if (g.isEditAllowed) "true" else "false" } />) }
      </refGrids>
      resultsb.append(grids.toString())
    }
    if(rs.relations!=null){
      resultsb.append("<relationData>"+ rs.relations.map( f =>f.toXml.toString()).mkString + "</relationData>")
    }
    if(rs.detailGrids!=null)
    {
      resultsb.append(this.tag("detailGridsData",rs.detailGrids.map(f => "<detailGrid><id>"+f.windowGridId+"</id><data>"+ convertDataTable(f).toString() + "</data></detailGrid>").mkString("\n")))
    }
    if (sectionSettings != null) {
      val sections =
        <sections>
          { sectionSettings.map(g => g.toXml)}
        </sections>

      resultsb.append(sections.toString())
    }
    resultsb.append("<Action>")
    if(rs.defaultTabId!=null) resultsb.append("<OpenTab>"+rs.defaultTabId+"</OpenTab>")
    resultsb.append("</Action>")
    if(execStatProvider!=null)resultsb.append(execStatProvider.toXml)
    val result = resultsb.append("</result>")
    result.toString
  }

  /**
   * Получить список полей со значениями
   * из xml-параметра для редактирования записи
   *
   * Формат:
   * {{{
   * <editedField>
   *   <id></id>
   *   <value></value>
   * </editedField>
   * }}}
   */
  private def convertDataTable(rs : DataViewTable,hiddenFields: Set[String] = Set()):StringBuilder = {
    def toNode(column: String, content: Any, properties: Map[String, Any], encode: Boolean) = {
      val mangledCol = column
        .replaceAll("\\$$", "__display_string")
      //  .replaceAll("\\$Color", "__color")
       // .replaceAll("_Color", "__color")
      val encString =
        if (encode && content != null)
          Base64.encodeBytes(content.asInstanceOf[Array[Byte]])
        else Option(content)
          .getOrElse("").toString
          .replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")

      val attrs = properties.foldLeft(new StringBuilder())((sb, prop) => {
        val (pCode, pValue) = prop

        def attrToStr(c: String, v: Any) =
          String.format(" %1$s=\"%2$s\"", c, v.toString)

        val attr = if (pValue.isInstanceOf[Option[Boolean]]) pValue.asInstanceOf[Option[Boolean]]
          .map(v => attrToStr(pCode, v))
          .getOrElse("")
        else attrToStr(pCode, pValue)

        sb.append(attr)
      }).toString

      String.format("<%1$s%3$s>%2$s</%1$s>", mangledCol, encString, attrs)
    }

    def formString(sb: StringBuilder, row: DataViewRow): StringBuilder = {
      sb.append("<object")
      if(row.position!=null) {
        sb.append(Model2Xml.attribute("x",row.position.x))
        sb.append(Model2Xml.attribute("y",row.position.y))
        sb.append(Model2Xml.attribute("width",row.position.width))
        sb.append(Model2Xml.attribute("height",row.position.height))
      }
      sb.append(">")
      if (row.data != null) {
        val binSet = Set(rs.binaryItems: _*)
        val pairs = (rs.columns zip row.data).zipWithIndex

        pairs.foreach(_ match { case ((column, content), n) =>
          // проверяем, что есть значение ячейки
          if ((content != null || row.fieldOverrides != null) && !(hiddenFields contains column)) {
            val enc = binSet contains (n + 1)
            val properties =
              if (row.fieldOverrides != null && row.fieldOverrides.contains(column))
                Option(row.fieldOverrides(column)).getOrElse(Map[String, Any]())
              else Map[String, Any]()

            val propWithColor = if (row.cellColors(n) != null)
              properties + ("color" -> row.cellColors(n))
            else properties

            if (!propWithColor.isEmpty || content != null)
              sb.append(toNode(column, content, propWithColor, enc))
          }
        })
      }

      // добавляем атрибут цвета
      if (row.rowColor != null) {
        sb.append(this.tag("__color",row.rowColor.color))
        sb.append(this.tag("__color2",row.rowColor.color2))
      }
       // sb.append(String.format("<__color>%s</__color>", row.rowColor.color))
      if(row.isDeleteNotAllowed.isDefined)
        sb.append("<__isDeleteNotAllowed>"+row.isDeleteNotAllowed.get.toString+ "</__isDeleteNotAllowed>")
      sb.append(this.tag("__redefinition",
        if(row.menuOverrides.length > 0) this.tag("menus",row.menuOverrides.map(_.xmlString).mkString("\n")) else null))
      sb.append("</object>")
    }
    val startsb = new StringBuilder("")
    val resultsb = if (rs != null) rs.data.foldLeft(startsb) { (sb, row) =>
      formString(sb, row)
    }
    else startsb
    resultsb
  }

  def parseEditReq(xml_str: String): Map[String, String] = {
    val xml = XML.loadString(xml_str)

    val fields = (xml \\ "editedField")
    val result = scala.collection.mutable.Map[String,String]()
     fields.foreach(field => {
        val idField = (field \\ "id")(0).text
        val valueField = try {
          val fieldTag = (field \\ "value")(0)
          if (fieldTag == null)
            null
          else {
            val nullAtr = fieldTag.asInstanceOf[Elem].attributes.find(x => x.key == "nil")
            if (nullAtr != None && nullAtr.get.value.text == "true")
              null
            else
              fieldTag.text
          }
        } catch {
          case _ => null
        }
        result += idField->valueField
      })
    result.toMap
  }

  /**
   * Очистить xml от xml-заголовка
   */
  def clearXmlFromHeader(xmlStr: String): String = {
    val xml = XML.loadString(xmlStr)
    xml.toString
  }

  def parseTreeVars(xmlStr: String): (String, String) = {
    val xml = XML.loadString(xmlStr)
    val vars = xml \\ "vars" \\ "variables"
    val treeVars = xml \\ "vars" \\ "treeVariables"
    (vars.toString, treeVars.toString.replaceFirst("<treeVariables>", "<variables>").replaceFirst("</treeVariables>", "</variables>"))
  }

}
object Model2Xml{
   def tag(tag: String, obj: Object): String = {
    if (obj == null) return ""

    var result = new StringBuilder()
    result.append("<").append(tag).append(">")
      .append(obj.toString())
      .append("</").append(tag).append(">")

    result.toString()
  }
  def attribute( attribute:String, obj:Any):String = {
    if(obj == null) "" else  " " + attribute + "=\"" + obj.toString +"\""
  }
}
