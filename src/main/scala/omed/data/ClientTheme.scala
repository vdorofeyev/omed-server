package omed.data

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 02.12.13
 * Time: 13:10
 * To change this template use File | Settings | File Templates.
 */
case class ClientTheme (val id:String, val name :String,val code :String, params:Map[String,Any],val groups:Seq[ClientThemeGroup]) {
    def toXml:String={
        val xml =
          <theme>
            <id>{id}</id>
            <name>{name}</name>
            <code>{code}</code>
            <params>
            {params.map(f=>
              <param>
                <code>{f._1}</code>
                <value>{f._2.toString}</value>
              </param>)}
            </params>
            <buttonGroups>
              {
                groups.map(f =>
              <buttonGroup>
                  <id>{f.id}</id>
                  <name>{f.name}</name>
                  <params> {
                    f.params.map(p =>
                      <param>
                        <code>{p._1}</code>
                        <value>{p._2}</value>
                      </param>)
                    }
                  </params>
                </buttonGroup>)
              }
            </buttonGroups>
          </theme>
      xml.toString()
    }
}

case class ClientThemeGroup(val id:String,val name :String, val params:Map[String,Any])