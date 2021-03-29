package org.grapheco.lynx

import org.junit.{Assert, Test}
import org.opencypher.v9_0.util.symbols.CypherType

import scala.collection.mutable

class CypherCreateTest extends TestBase {
  @Test
  def testCreateNode(): Unit = {
    var rs = runOnDemoGraph("CREATE ()")
    Assert.assertEquals(NODE_SIZE + 1, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)

    rs = runOnDemoGraph("CREATE (n)")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)

    //should invoke CREATE even if result not retrieved
    runner.run("CREATE (n)", Map.empty)
    Assert.assertEquals(NODE_SIZE + 3, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)
  }

  @Test
  def testCreate2Nodes(): Unit = {
    var rs = runOnDemoGraph("CREATE (),()")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)

    rs = runOnDemoGraph("CREATE (n),(m)")
    Assert.assertEquals(NODE_SIZE + 4, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)

    //should invoke CREATE even if result not retrieved
    runner.run("CREATE (n),(m)", Map.empty)
    Assert.assertEquals(NODE_SIZE + 6, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)
  }

  @Test
  def testCreateRelation(): Unit = {
    var rs = runOnDemoGraph("CREATE ()-[:KNOWS]->()")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 1, all_rels.size)
  }

  @Test
  def testCreate2Relations(): Unit = {
    var rs = runOnDemoGraph("CREATE ()-[:KNOWS]->(),()-[:KNOWS]->()")
    Assert.assertEquals(NODE_SIZE + 4, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size)
  }

  @Test
  def testCreate2RelationsInChain(): Unit = {
    var rs = runOnDemoGraph("CREATE ()-[:KNOWS]->()-[:KNOWS]->()")
    Assert.assertEquals(NODE_SIZE + 3, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size)
  }

  @Test
  def testCreateNamedRelationsInChain(): Unit = {
    var rs = runOnDemoGraph("CREATE (m)-[:KNOWS]->(n)-[:KNOWS]->(t)")
    Assert.assertEquals(NODE_SIZE + 3, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size)
  }

  @Test
  def testCreateNamedExistingNodeInChain(): Unit = {
    var rs = runOnDemoGraph("CREATE (m)-[:KNOWS]->(n)-[:KNOWS]->(m)")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size)
  }

  @Test
  def testCreateNodeWithProperties(): Unit = {
    val rs = runOnDemoGraph("CREATE (n {name: 'God', age: 10000})")
    Assert.assertEquals(NODE_SIZE + 1, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)
    Assert.assertEquals(LynxValue("God"), all_nodes.apply(NODE_SIZE).property("name").get)
  }

  @Test
  def testCreateNodeWithReturn(): Unit = {
    val rs = runOnDemoGraph("CREATE (n {name: 'God', age: 10000}) return n")
    Assert.assertEquals(NODE_SIZE + 1, all_nodes.size)
    Assert.assertEquals(REL_SIZE, all_rels.size)
    Assert.assertEquals((NODE_SIZE + 1).toLong, rs.records.toSeq.apply(0).apply("n").asInstanceOf[LynxNode].id.value)
  }

  @Test
  def testCreateNodesRelation(): Unit = {
    val rs = runOnDemoGraph("CREATE (n:person {name: 'God', age: 10000}), (m:place {name: 'heaven'}), (n)-[r:livesIn]->(m) return n,r,m")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 1, all_rels.size)

    Assert.assertEquals(LynxString("God"), all_nodes(NODE_SIZE).properties("name"))
    Assert.assertEquals(LynxInteger(10000), all_nodes(NODE_SIZE).properties("age"))
    Assert.assertEquals(Seq("person"), all_nodes(NODE_SIZE).labels)

    Assert.assertEquals(LynxString("heaven"), all_nodes(NODE_SIZE + 1).properties("name"))
    Assert.assertEquals(Seq("place"), all_nodes(NODE_SIZE + 1).labels)

    Assert.assertEquals("livesIn", all_rels(REL_SIZE).relationType.get)
    Assert.assertEquals(all_nodes(NODE_SIZE + 1).id.value, all_rels(REL_SIZE).startId)
    Assert.assertEquals(all_nodes(NODE_SIZE + 1).id.value, all_rels(REL_SIZE).endId)
  }

  @Test
  def testCreateNodesAndRelationsWithinPath(): Unit = {
    val rs = runOnDemoGraph("CREATE (n:person {name: 'God', age: 10000})-[r:livesIn]->(m:place {name: 'heaven'}) return n,r,m")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 1, all_rels.size)

    Assert.assertEquals(LynxString("God"), all_nodes(NODE_SIZE).properties("name"))
    Assert.assertEquals(LynxInteger(10000), all_nodes(NODE_SIZE).properties("age"))
    Assert.assertEquals(Seq("person"), all_nodes(NODE_SIZE).labels)

    Assert.assertEquals(LynxString("heaven"), all_nodes(NODE_SIZE + 1).properties("name"))
    Assert.assertEquals(Seq("place"), all_nodes(NODE_SIZE + 1).labels)

    Assert.assertEquals("livesIn", all_rels(REL_SIZE).relationType.get)
    Assert.assertEquals(all_nodes(NODE_SIZE + 1).id.value, all_rels(REL_SIZE).startId)
    Assert.assertEquals(all_nodes(NODE_SIZE + 1).id.value, all_rels(REL_SIZE).endId)
  }

  @Test
  def testCreateNodesPath(): Unit = {
    val rs = runOnDemoGraph("CREATE (a:person {name: 'BaoChai'}), (b:person {name: 'BaoYu'}), (c:person {name: 'DaiYu'}), (a)-[:LOVES]->(b)-[:LOVES]->(c) return a,b,c")
    Assert.assertEquals(NODE_SIZE + 3, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size)

    Assert.assertEquals(LynxString("BaoChai"), all_nodes(NODE_SIZE).properties("name"))
    Assert.assertEquals(LynxString("BaoYu"), all_nodes(NODE_SIZE + 1).properties("name"))
    Assert.assertEquals(LynxString("DaiYu"), all_nodes(NODE_SIZE + 2).properties("name"))

    Assert.assertEquals("LOVES", all_rels(REL_SIZE).relationType.get)
    Assert.assertEquals(all_nodes(NODE_SIZE).id.value, all_rels(REL_SIZE).startId)
    Assert.assertEquals(all_nodes(NODE_SIZE + 1).id.value, all_rels(REL_SIZE).endId)

    Assert.assertEquals("LOVES", all_rels(REL_SIZE + 1).relationType.get)
    Assert.assertEquals(all_nodes(NODE_SIZE + 1).id.value, all_rels(REL_SIZE + 1).startId)
    Assert.assertEquals(all_nodes(NODE_SIZE + 2).id.value, all_rels(REL_SIZE + 1).endId)
  }

  @Test
  def testMatchToCreateRelation(): Unit = {
    var rs = runOnDemoGraph("match (m:person {name:'bluejoe'}),(n {name:'CNIC'}) CREATE (m)-[r:WORKS_FOR]->(n) return m,r,n")
    Assert.assertEquals(NODE_SIZE, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 1, all_rels.size)

    Assert.assertEquals("WORKS_FOR", all_rels(REL_SIZE).relationType.get)
    Assert.assertEquals(1.toLong, all_rels(REL_SIZE).startId)
    Assert.assertEquals(3.toLong, all_rels(REL_SIZE).endId)
  }

  @Test
  def testMatchToCreateMultipleRelation(): Unit = {
    var rs = runOnDemoGraph("match (m:person),(n {name:'CNIC'}) CREATE (m)-[r:WORKS_FOR]->(n) return m,r,n")
    Assert.assertEquals(NODE_SIZE, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size) //2 persons

    Assert.assertEquals("WORKS_FOR", all_rels(REL_SIZE).relationType.get)
    Assert.assertEquals(1.toLong, all_rels(REL_SIZE).startId)
    Assert.assertEquals(3.toLong, all_rels(REL_SIZE).endId)
  }

  @Test
  def testMatchToCreateMultipleNodesAndRelations(): Unit = {
    var rs = runOnDemoGraph("match (m:person) CREATE (n {name: 'God', age: 10000}), (n)-[r:LOVES]->(m) return n,r,m")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size)

    Assert.assertEquals(LynxString("God"), all_nodes(NODE_SIZE).properties("name"))
    Assert.assertEquals(LynxInteger(10000), all_nodes(NODE_SIZE).properties("age"))

    Assert.assertEquals(LynxString("God"), all_nodes(NODE_SIZE + 1).properties("name"))
    Assert.assertEquals(LynxInteger(10000), all_nodes(NODE_SIZE + 1).properties("age"))

    Assert.assertEquals("LOVES", all_rels(REL_SIZE).relationType.get)
    Assert.assertEquals((NODE_SIZE + 1).toLong, all_rels(REL_SIZE).startId)
    Assert.assertEquals(1.toLong, all_rels(REL_SIZE).endId)

    Assert.assertEquals("LOVES", all_rels(REL_SIZE + 1).relationType.get)
    Assert.assertEquals((NODE_SIZE + 2).toLong, all_rels(REL_SIZE + 1).startId)
    Assert.assertEquals(2.toLong, all_rels(REL_SIZE + 1).endId)
  }

  @Test
  def testMatchToCreateMultipleNodesAndRelationsWithExpr(): Unit = {
    var rs = runOnDemoGraph("match (m:person) CREATE (n {name: 'clone of '+m.name, age: m.age+1}), (n)-[r:IS_CLONE_OF]->(m) return n,r,m")
    Assert.assertEquals(NODE_SIZE + 2, all_nodes.size)
    Assert.assertEquals(REL_SIZE + 2, all_rels.size)

    Assert.assertEquals(LynxString("clone of bluejoe"), all_nodes(NODE_SIZE).properties("name"))
    Assert.assertEquals(LynxInteger(41), all_nodes(NODE_SIZE).properties("age"))

    Assert.assertEquals(LynxString("clone of alex"), all_nodes(NODE_SIZE + 1).properties("name"))
    Assert.assertEquals(LynxInteger(31), all_nodes(NODE_SIZE + 1).properties("age"))

    Assert.assertEquals("IS_CLONE_OF", all_rels(REL_SIZE).relationType.get)
    Assert.assertEquals((NODE_SIZE + 1).toLong, all_rels(REL_SIZE).startId)
    Assert.assertEquals(1.toLong, all_rels(REL_SIZE).endId)
    Assert.assertEquals("IS_CLONE_OF", all_rels(REL_SIZE + 1).relationType.get)
    Assert.assertEquals((NODE_SIZE + 2).toLong, all_rels(REL_SIZE + 1).startId)
    Assert.assertEquals(2.toLong, all_rels(REL_SIZE + 1).endId)
  }

  @Test
  def testCreateIndex(): Unit = {
    runOnDemoGraph("CREATE (n:person {name: 'God', age: 10000}), (m:place {name: 'heaven'}), (n)-[r:livesIn]->(m) return n,r,m")
    runOnDemoGraph("CREATE INDEX ON :person(name)")
    runOnDemoGraph("CREATE INDEX ON :person(name, age)")
  }


  @Test
  def testCreateDate(): Unit ={
    runOnDemoGraph("CREATE (n:Person {name:'node_Date1',born:date('2018-04-05')}) return n")
  }

  @Test
  def testFunction(): Unit ={
    runOnDemoGraph("return toInterger('345')")
  }

  @Test
  def testFunction23(): Unit ={
    runOnDemoGraph("return 2,toInterger('345'), date('2018-05-06')")
  }

  @Test
  def testFunction2(): Unit ={
    runOnDemoGraph("match (n) return group by  ")
  }


  @Test
  def testSeq(): Unit ={
    var sq: mutable.IndexedSeq[(Int, String)] = mutable.IndexedSeq[(Int, String)]()

    sq = sq ++ Seq(1 -> "1")
    sq = sq ++ Seq(2 -> "2")
    sq = sq ++ Seq(3 -> "3")
    sq = sq ++ Seq(1 -> "5")

    println(sq.toMap.get(1))

  }

  @Test
  def testGroupBy(): Unit ={
    val arr:List[(Int, Int, Int)] =List((1,2,3),(3,4,5),(2,4,6),(1,2,1));
/*    val res = arr.groupBy(_._1).mapValues{
      l =>
        val r = l.reduce((a,b) => (0, a._2 + b._2, a._3 + b._3))
        (r._2, r._3 * 1.0 / l.size)
    }*/
    val col = List[Int](0,1)
    val res = arr.groupBy(r => col.map(r.productElement))
      .mapValues(l => l
        .map(a => (a._2, a._3 * 1.0 / l.size))
        .reduce((a,b) => (a._1 + b._1, a._2 + b._2)))
/*    val res = arr.foldLeft(Map(0->(0, 0))){
      (f,s) => {
        //println(f)
        //val res1 = (f(s._1)._2 +1, f(s._1)._2 + s._3*1.0/arr.size)

       f.updated(s._1, (f(s._1)._2 +1, f(s._1)._2 + s._3/arr.size))
       // f.
        //(f._1 + s._1, f._2+1, f._3 + s._3*1.0/arr.size)
      }
    }*/

    println(res)
    println(arr.size)
  }

  @Test
  def testAff(): Unit ={
    val arr =List((1,2,3),(3,4,5),(2,4,6),(1,2,1))
   // arr.groupBy(_._1).aggregate().c
  }

  @Test
  def testmatch(): Unit ={
    runOnDemoGraph("match (n{name:'alex'}) return n")
  }

  @Test
  def testmatchxing(): Unit ={
    runOnDemoGraph("match data =(:leader)-[:KNOWS*3..2]->() return data")
  }

  @Test
  def testmatchxing2(): Unit ={
    runOnDemoGraph("match (n:leader)-[:KNOWS*3..2]->() return n")
  }




}