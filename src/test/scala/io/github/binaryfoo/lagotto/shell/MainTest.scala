package io.github.binaryfoo.lagotto.shell

import java.io.{File, ByteArrayOutputStream}

import io.github.binaryfoo.lagotto.LogReader
import org.scalatest.{FlatSpec, Matchers}

class MainTest extends FlatSpec with Matchers {

  "Main" should "produce tab separated output with --tsv option" in {
    val output = run("--tsv", "time,mti,11", "src/test/resources/a-bunch.xml")
    output shouldEqual """time	mti	11
                         |00:00:03.292	0200	1
                         |00:00:04.292	0200	2
                         |00:00:04.892	0210	2
                         |00:00:04.992	0210	1
                         |""".stripMargin
  }

  it should "produce jira formatted table with --jira-table option" in {
    val output = run("--jira-table", "time,mti,11", "src/test/resources/a-bunch.xml")
    output shouldEqual """||time||mti||11||
|00:00:03.292|0200|1|
|00:00:04.292|0200|2|
|00:00:04.892|0210|2|
|00:00:04.992|0210|1|
"""
  }

  it should "produce a HTML formatted table with --html option" in {
    val output = run("--html", "time,mti,11", "src/test/resources/a-bunch.xml")
    output should include ("<table>")
    output should include ("<tr><th>time</th><th>mti</th><th>11</th></tr>")
    output should include ("<tr><td>00:00:03.292</td><td>0200</td><td>1</td></tr>")
    output should include ("</table>")
  }

  it should "include delay between messages with delay field in --csv option" in {
    val output = run("-f", "socket=10.0.0.1:4321", "--csv", "time,mti,11,delay", "src/test/resources/a-bunch.xml")
    output shouldEqual """time,mti,11,delay
                         |00:00:03.292,0200,1,0
                         |00:00:04.992,0210,1,1700
                         |""".stripMargin
  }

  it should "allow filtering on delay between messages" in {
    val output = run("-f", "socket=10.0.0.1:4321", "-f", "delay>1000", "--csv", "time,mti,11,delay", "src/test/resources/a-bunch.xml")
    output shouldEqual """time,mti,11,delay
                         |00:00:04.992,0210,1,1700
                         |""".stripMargin
  }

  it should "allow sort on delay between messages" in {
    val output = run("-f", "socket=10.0.0.1:4321", "--csv", "time,mti,11,delay", "--sort-desc", "delay", "src/test/resources/a-bunch.xml")
    output shouldEqual """time,mti,11,delay
                         |00:00:04.992,0210,1,1700
                         |00:00:03.292,0200,1,0
                         |""".stripMargin
  }

  it should "group rows when count field included in --csv option" in {
    val output = run("--csv", "time(mm:ss),count", "src/test/resources/a-bunch.xml")
    output shouldEqual """time(mm:ss),count
                          |00:03,1
                          |00:04,3
                          |""".stripMargin
  }

  it should "filter on group count with --field count>N" in {
    val output = run("--csv", "time(mm:ss),count", "--field", "count>2", "src/test/resources/a-bunch.xml")
    output shouldEqual """time(mm:ss),count
                          |00:04,3
                          |""".stripMargin
  }

  it should "filter on group count(condition) with --field count>N" in {
    val output = run("--csv", "time(mm:ss),count(time(mm:ss)=00:03)", "--field", "count(time(mm:ss)=00:03)>1", "src/test/resources/a-bunch.xml")
    output shouldEqual """time(mm:ss),count(time(mm:ss)=00:03)
                          |00:03,1
                          |""".stripMargin
  }

  it should "count only if condition is true for count(39=01) in --csv option" in {
    val output = run("--csv", "time(mm:ss),count(39=01),count(39=00)", "src/test/resources/a-bunch.xml")
    output shouldEqual """time(mm:ss),count(39=01),count(39=00)
                          |00:03,0,0
                          |00:04,1,1
                          |""".stripMargin
  }

  it should "support mysql like group_concat() in --csv option" in {
    val output = run("--csv", "time(mm:ss),group_concat(48.1(/.*#//))", "src/test/resources/a-bunch.xml")
    output shouldEqual """time(mm:ss),group_concat(48.1(/.*#//))
                         |00:03,1
                         |00:04,2,3,4
                         |""".stripMargin
  }

  it should "allow sort on count field included in --csv option" in {
    val output = run("--csv", "time(mm:ss),count", "--sort-desc", "count", "src/test/resources/a-bunch.xml")
    output shouldEqual """time(mm:ss),count
                         |00:04,3
                         |00:03,1
                         |""".stripMargin
  }

  it should "group rows when max(lifespan) field included in --csv option" in {
    val output = run("--csv", "ipAddress,max(lifespan)", "src/test/resources/a-pair.xml")
    output shouldEqual """ipAddress,max(lifespan)
                          |10.0.0.1,10005
                          |,
                          |""".stripMargin
  }

  it should "group rows when max(lifespan) field included in --jira-table option" in {
    val output = run("--jira-table", "ipAddress,max(lifespan)", "src/test/resources/a-pair.xml")
    output shouldEqual """||ipAddress||max(lifespan)||
|10.0.0.1|10005|
|||
"""
  }

  it should "group rows when min(lifespan) field included in --csv option" in {
    val output = run("--csv", "ipAddress,min(lifespan)", "src/test/resources/a-pair.xml")
    output shouldEqual """ipAddress,min(lifespan)
                          |10.0.0.1,1000
                          |,
                          |""".stripMargin
  }

  it should "group rows when sum(lifespan) field included in --csv option" in {
    val output = run("--csv", "ipAddress,sum(lifespan)", "src/test/resources/a-pair.xml")
    output shouldEqual """ipAddress,sum(lifespan)
                          |10.0.0.1,11005
                          |,
                          |""".stripMargin
  }

  it should "leave out the header with --no-header option" in {
    val output = run("--no-header", "--csv", "time,mti,11", "src/test/resources/basic.xml")
    output shouldEqual """00:00:03.292,0800,28928
                         |13:10:55.000,,
                         |""".stripMargin
  }

  it should "filter on exact field match with -f option" in {
    val output = run("-f", "11=28928", "src/test/resources/basic.xml")
    output shouldEqual """<log realm="some.channel/10.0.0.1:4321" at="Mon Nov 24 00:00:03 EST 2014.292" lifespan="10005ms">
                         |  <receive>
                         |    <isomsg direction="incoming">
                         |      <!-- org.jpos.iso.packager.XMLPackager -->
                         |      <field id="0" value="0800"/>
                         |      <field id="7" value="1124000003"/>
                         |      <field id="11" value="28928"/>
                         |      <field id="24" value="831"/>
                         |    </isomsg>
                         |  </receive>
                         |</log>
                         |""".stripMargin
  }

  it should "produce pair entries with --pair option" in {
    val output = run("--pair", "--csv", "time,mti,11,rtt", "src/test/resources/a-bunch.xml")
    // output ends up in ordered by response received time...
    output shouldEqual """time,mti,11,rtt
                         |00:00:04.292,0200,2,600
                         |00:00:03.292,0200,1,1700
                         |""".stripMargin
  }

  it should "sort by --sort option" in {
    val output = run("--sort", "time", "--pair", "--csv", "time,mti,11,rtt", "src/test/resources/a-bunch.xml")
    output shouldEqual """time,mti,11,rtt
                         |00:00:03.292,0200,1,1700
                         |00:00:04.292,0200,2,600
                         |""".stripMargin
  }

  it should "reverse sort with --sort-desc option" in {
    val output = run("--sort-desc", "11", "--pair", "--csv", "time,mti,11,rtt", "src/test/resources/a-bunch.xml")
    output shouldEqual """time,mti,11,rtt
                         |00:00:04.292,0200,2,600
                         |00:00:03.292,0200,1,1700
                         |""".stripMargin
  }

  it should "filter using greater than operator with -f rtt>1000 option" in {
    val output = run("--pair", "-f", "rtt>1000", "--csv", "time,mti,11,rtt", "src/test/resources/a-bunch.xml")
    output shouldEqual """time,mti,11,rtt
                         |00:00:03.292,0200,1,1700
                         |""".stripMargin
  }

  it should "filter using less than operator with -f rtt<1000 option" in {
    val output = run("--pair", "-f", "rtt<1000", "--csv", "time,mti,11,rtt", "src/test/resources/a-bunch.xml")
    output shouldEqual """time,mti,11,rtt
                         |00:00:04.292,0200,2,600
                         |""".stripMargin
  }

  it should "filter using case insensitive contains operator with -f 48.1~suBfield option" in {
    val output = run("-f", "48.1~suBfield", "--csv", "time,mti,11,file", "src/test/resources/a-pair.xml")
    output shouldEqual """time,mti,11,file
                         |00:00:04.100,0810,28928,a-pair.xml:14
                         |""".stripMargin
  }

  it should "negate filter with !in -f 24!=831 option" in {
    val output = run("-f", "24!=831", "src/test/resources/a-pair.xml")
    output shouldEqual """<log realm="rotate-log-listener" at="Mon Nov 24 13:10:55 EST 2014">
                         |   maxSize (50000000) threshold reached
                         |</log>
                         |""".stripMargin
  }

  it should "match anywhere in the message with -grep option" in {
    val output = run("--grep", "threshold", "src/test/resources/a-pair.xml")
    output shouldEqual """<log realm="rotate-log-listener" at="Mon Nov 24 13:10:55 EST 2014">
                         |   maxSize (50000000) threshold reached
                         |</log>
                         |""".stripMargin
  }

  it should "negate --grep with --grep! option" in {
    val output = run("--grep!", "threshold", "src/test/resources/a-pair.xml")
    output shouldEqual """<log realm="some.channel/10.0.0.1:4321" at="Mon Nov 24 00:00:03 EST 2014.292" lifespan="10005ms">
                         |  <send>
                         |    <isomsg direction="outgoing">
                         |      <!-- org.jpos.iso.packager.XMLPackager -->
                         |      <field id="0" value="0800"/>
                         |      <field id="7" value="1124000003"/>
                         |      <field id="11" value="28928"/>
                         |      <field id="24" value="831"/>
                         |    </isomsg>
                         |  </send>
                         |</log>
                         |<log realm="some.channel/10.0.0.1:4321" at="Mon Nov 24 00:00:04 EST 2014.100" lifespan="1000ms">
                         |  <receive>
                         |    <isomsg direction="incoming">
                         |      <!-- org.jpos.iso.packager.XMLPackager -->
                         |      <field id="0" value="0810"/>
                         |      <field id="7" value="1124000003"/>
                         |      <field id="11" value="28928"/>
                         |      <field id="24" value="831"/>
                         |      <isomsg id="48">
                         |        <field id="1" value="subfield 48.1"/>
                         |      </isomsg>
                         |    </isomsg>
                         |  </receive>
                         |</log>
                         |""".stripMargin
  }

  it should "print full text by default" in {
    val output = run("src/test/resources/a-pair.xml")
    output shouldEqual """<log realm="some.channel/10.0.0.1:4321" at="Mon Nov 24 00:00:03 EST 2014.292" lifespan="10005ms">
                         |  <send>
                         |    <isomsg direction="outgoing">
                         |      <!-- org.jpos.iso.packager.XMLPackager -->
                         |      <field id="0" value="0800"/>
                         |      <field id="7" value="1124000003"/>
                         |      <field id="11" value="28928"/>
                         |      <field id="24" value="831"/>
                         |    </isomsg>
                         |  </send>
                         |</log>
                         |<log realm="some.channel/10.0.0.1:4321" at="Mon Nov 24 00:00:04 EST 2014.100" lifespan="1000ms">
                         |  <receive>
                         |    <isomsg direction="incoming">
                         |      <!-- org.jpos.iso.packager.XMLPackager -->
                         |      <field id="0" value="0810"/>
                         |      <field id="7" value="1124000003"/>
                         |      <field id="11" value="28928"/>
                         |      <field id="24" value="831"/>
                         |      <isomsg id="48">
                         |        <field id="1" value="subfield 48.1"/>
                         |      </isomsg>
                         |    </isomsg>
                         |  </receive>
                         |</log>
                         |<log realm="rotate-log-listener" at="Mon Nov 24 13:10:55 EST 2014">
                         |   maxSize (50000000) threshold reached
                         |</log>
                         |""".stripMargin
  }

  it should "include N messages preceding a match with -B N option" in {
    val output = run("-B", "1", "--csv", "48.1", "--no-header", "--grep", "#3", "src/test/resources/a-bunch.xml")
    output shouldEqual
      """a-bunch.xml #2
        |a-bunch.xml #3
        |""".stripMargin
  }

  it should "include N messages following a match with -A N option" in {
    val output = run("-A", "1", "--csv", "48.1", "--no-header", "--grep", "#2", "src/test/resources/a-bunch.xml")
    output shouldEqual
      """a-bunch.xml #2
        |a-bunch.xml #3
        |""".stripMargin
  }

  it should "include N messages before and after a match with -C N option" in {
    val output = run("-C", "1", "--csv", "48.1", "--no-header", "--grep", "#3", "src/test/resources/a-bunch.xml")
    output shouldEqual
      """a-bunch.xml #2
        |a-bunch.xml #3
        |a-bunch.xml #4
        |""".stripMargin
  }

  it should "includes preceding context only once with -B N option" in {
    val output = run("-B", "1", "--csv", "48.1", "--no-header", "--field", "at~00:00:04", "src/test/resources/a-bunch.xml")
    output shouldEqual
      """a-bunch.xml #1
        |a-bunch.xml #2
        |a-bunch.xml #3
        |a-bunch.xml #4
        |""".stripMargin
  }

  it should "include records matching all -f filters" in {
    val output = run("-f", "48.1~bunch", "-f", "at~.892", "--csv", "48.1", "--no-header", "src/test/resources/a-bunch.xml")
    output shouldEqual
      """a-bunch.xml #3
        |""".stripMargin
  }

  it should "print wrap pairs in <pair></pair> with --pair" in {
    val output = run("--pair","src/test/resources/a-pair.xml")
    output shouldEqual """<pair>
                         |<log realm="some.channel/10.0.0.1:4321" at="Mon Nov 24 00:00:03 EST 2014.292" lifespan="10005ms">
                         |  <send>
                         |    <isomsg direction="outgoing">
                         |      <!-- org.jpos.iso.packager.XMLPackager -->
                         |      <field id="0" value="0800"/>
                         |      <field id="7" value="1124000003"/>
                         |      <field id="11" value="28928"/>
                         |      <field id="24" value="831"/>
                         |    </isomsg>
                         |  </send>
                         |</log>
                         |<log realm="some.channel/10.0.0.1:4321" at="Mon Nov 24 00:00:04 EST 2014.100" lifespan="1000ms">
                         |  <receive>
                         |    <isomsg direction="incoming">
                         |      <!-- org.jpos.iso.packager.XMLPackager -->
                         |      <field id="0" value="0810"/>
                         |      <field id="7" value="1124000003"/>
                         |      <field id="11" value="28928"/>
                         |      <field id="24" value="831"/>
                         |      <isomsg id="48">
                         |        <field id="1" value="subfield 48.1"/>
                         |      </isomsg>
                         |    </isomsg>
                         |  </receive>
                         |</log>
                         |</pair>
                         |""".stripMargin
  }

  it should "filter on lifespan greater than a number" in {
    val output = run("--field", "lifespan>1001", "--csv", "mti,lifespan", "src/test/resources/a-pair.xml")
    output shouldBe
      """mti,lifespan
        |0800,10005
        |""".stripMargin
  }

  it should "filter on lifespan less than a number" in {
    val output = run("--field", "lifespan<1000", "--csv", "mti,lifespan", "src/test/resources/a-pair.xml")
    output shouldBe
      """mti,lifespan
        |0810,1000
        |,
        |""".stripMargin
  }

  "Given a <log> record garbled by a 2nd partial within it" should "keep on truckin'" in {
    val output = run("--csv", "0,11", "src/test/resources/message-inception-garbage.xml")
    output shouldEqual """0,11
                          |2804,
                          |2814,127
                          |""".stripMargin
  }

  "With --strict garbled input" should "be rejected" in {
    the [IllegalArgumentException] thrownBy {
      run("--strict", "src/test/resources/message-inception-garbage.xml")
    } should have message """Unexpected <log> start tag. Line message-inception-garbage.xml:5:             <field id="0" value="2804"/><log realm="remoteLink.channel" at="Mon Dec 08 14:51:55 EST 2014.648">"""
  }

  def run(args: String*): String = {
    val out = new ByteArrayOutputStream()
    Console.withOut(out) {
      Main.main(args.toArray)
    }
    out.toString
  }
}
