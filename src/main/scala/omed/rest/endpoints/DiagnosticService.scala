package omed.rest.endpoints

import javax.ws.rs.{ Produces, GET, Path }
import scala.collection.JavaConversions._
import com.google.inject.Inject
import scala.Array
import javax.ws.rs.core.{Response, MediaType}
import java.util.Properties
import omed.db.DBProfiler
import omed.bf.BusinessFunctionLogger
import omed.model.{EntityInstance, SimpleValue, Value}
import scala.xml.Elem
import java.util.logging.Logger

/**
 * Сервис диагностики работы серверного приложения и хранилища данных
 */
@Path("/diag")
class DiagnosticService {

  @Inject
  var businessFunctionLog :BusinessFunctionLogger = null
  /**
   * Формирует результат сбора статистики обращений к хранилищу данных
   * @return Ответ сервера с информацией о выполняемых запросах
   *         к хранилищу данных в виде HTML-документа
   */
  @GET
  @Path("/bf")
  @Produces(Array("text/html; charset=UTF-8"))
  def getBFStats = {

    val statHtml = businessFunctionLog.getAllLogs.map(x =>
      <div class="title">
        <span class="name"  style="cursor:pointer;"> {x.info} </span>
        {
          x.steps.map(f =>

          try{
            val tmp =
          <div class="sub">
          <div class="title level1"><span class="name"  style="cursor:pointer;">{f.descr}</span>
            <div class="sub">
              <div class="title level2">
                { if(f.context.isEmpty) <span class="name" >Контекст</span>
                 else
                <span class="name" style="text-decoration:underline;  cursor:pointer;">Контекст</span>
                <div class="sub"><table cellspacing="0" cellpadding="0" border="0"  bordercolor="#FFFF00"> {paramMapToXml(f.context) }</table></div>
                }

              </div>
              <div class="title level2">
                { if(f.params.isEmpty) <span class="name">Параметры</span>
                  else
                  <span class="name" style="text-decoration:underline; cursor:pointer;">Параметры</span>
                  <div class="sub"><table cellspacing="0" cellpadding="0" border="0"  bordercolor="#FFFF00"> {paramMapToXml(f.params) }</table></div>
                }
              </div>
            </div>
          </div>
        </div>

            tmp
          }
        catch {
          case _ => null
        }

          )
        //  x.steps.map(f => <b><tr><td>{f.descr}</td></tr> {f.context.map(f=> <tr><td>{f._1}</td><td>{valueToTable(f._2)}</td></tr>)}</b>)
        }
      </div>
    )

    <html>
      <head>
        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.6.2/jquery.min.js"></script>
        <style>
          .title {{  }}
          body>.title {{ margin: 25px 0 0 0; }}
          .sub {{ display: none; }}
          .level1{{ margin-left: 30px; color:blue; }}
          .level2{{ margin-left: 30px; color:green; }}
          .level3{{ margin-left: 30px;}}
          .level3 .val_line {{ color: magenta; margin-left: 0px; }}
          .level3 .val_lines {{ color: gray; margin-left: 30px; }}
          .name{{ font-weight: bold;  }}
          .value{{  }}
          span.name {{ padding-right: 20px; }}
          .var_val {{ color:gray; margin-left: 30px; }}
          .var_line {{ color:gray; margin-left: 30px; }}
          .var_lines {{ color:darkblue; margin-left: 30px; }}
        </style>
        <script type="text/javascript">
          $(document).ready(function(){{
          var bigger = String.fromCharCode(62);

          $('.title'+bigger+'.name').click(function(){{
          var title = $(this).parent();
          title.find(bigger+'.sub').toggle();
          return false;
          }});

          $('.val_line'+bigger+'.name').click(function(){{
          var level3 = $(this).closest('.level3');
          level3.find(bigger+'.sub').toggle();
          return false;
          }});


          }});
        </script>
      </head>
      <body>
        <p>Бизнесс-функции</p>

        { statHtml }
      </body>
    </html>.toString
  }
  def paramMapToXml(vars:Map[String,Value]):Iterable[Elem]={
      val t= vars.map(p=> valueToTable(p))
      if(t.isInstanceOf[Iterable[Elem]]) t
    else null

    //.filter(p=> p match {
    // case v:SimpleValue => v.data!=null
    //   case e:EntityInstance => true
    //  })
  }
  def valueToTable(variable:(String,Value)):Elem ={
   val t =
    variable._2 match {
      case v:SimpleValue =>  <tr><td valign="top"><div class="var_val"><span class="name">{if(variable._1!=null) variable._1 else ""}</span> </div></td><td><div class="var_val"><span class="value" style="white-space:pre">{if(v.data!=null) v.data else "[null]"}</span></div></td></tr>

      case e:EntityInstance => <div class="level3">
        <div class="val_line">
          <span class="name" style="cursor:pointer;">{variable._1}</span><span class="value">{ if(e.data!=null) e.getId else "[null]"}</span>
        </div>
        <div class="sub val_lines">
          {if(e.data!=null) e.data.toSeq.sortBy(f=>f._1).map(f=> <div><span class="name">{f._1}</span><span class="value">{if(f._2==null) "" else if(f._2.isInstanceOf[SimpleValue] && f._2.asInstanceOf[SimpleValue].data==null)"" else f._2}</span></div>)}
        </div>
      </div>
      case _ => <tr><td valign="top"> <div class="var_val"><span class="name">{variable._1}</span></div></td><td><div class="var_val"><span class="value">[null]</span></div></td></tr>
    }

    t
  }


  @GET
  @Path("/dropBF")
  @Produces(Array("text/html; charset=UTF-8"))
  def dropBFStat = {
    businessFunctionLog.dropLog
    val content = "<?xml version='1.0' encoding='utf-8'?>\n" +
      "<?xml-stylesheet type=\"text/css\" href=\"cache.css\"?>" +
      "<result>Лог БФ очищен</result>"

    Response.ok().entity(content).build()
  }
  //
  /**
   * Отдает версию серверного приложения
   * @return Ответ сервера с номером версии
   */


  @GET
  @Path("/db")
  @Produces(Array("text/html; charset=UTF-8"))
  def getDbStats = {
    val statHtml = DBProfiler.stats.toMap.toSeq.sortBy(_._1).map(x => x match {
      case (name, stat) => <tr>
        <td>
          { name }
        </td>
        <td class="number">
          { stat.calls }
        </td>
        <td class="number">
          { stat.totalTime / stat.calls }
        </td>
        <td class="number">
          { stat.maxTime }
        </td>
      </tr>
    })

    <html>
      <head>
        <style>
          .number {{ text-align: right; }}
        </style>
      </head>
      <body>
        <table>
          <tr>
            <th style="text-align: left;">Процедура</th>
            <th class="number">Кол-во вызовов</th>
            <th class="number">Среднее время, мс</th>
            <th class="number">Макс. время, мс</th>
          </tr>
          { statHtml }
        </table>
      </body>
    </html>.toString
  }

  /**
   * Отдает версию серверного приложения
   * @return Ответ сервера с номером версии
   */
  @GET
  @Path("/version")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getVersion = {

    val version = try {
      val props = new java.util.Properties()
      props.load(this.getClass().getResourceAsStream("/version.properties"))
      props.getProperty("version")
    }
    "<?xml version='1.0' encoding='utf-8'?>\n" +
      "<?xml-stylesheet type=\"text/css\" href=\"cache.css\"?>" +
      "<version>"+version+"</version>"

  }

}