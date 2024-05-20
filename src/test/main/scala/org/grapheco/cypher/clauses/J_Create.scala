package org.grapheco.cypher.clauses

import org.grapheco.lynx.TestBase
import org.grapheco.lynx.physical.{NodeInput, RelationshipInput}
import org.grapheco.lynx.types.LynxValue
import org.grapheco.lynx.types.structural.{LynxNode, LynxNodeLabel, LynxPropertyKey, LynxRelationship}
import org.junit.jupiter.api.{Assertions, BeforeEach, Test}
import scala.collection.mutable.ArrayBuffer

/**
 * @program: lynx
 * @description:
 * @author: LiamGao
 * @create: 2022-02-28 18:04
 */
class J_Create extends TestBase{
  val nodesInput = ArrayBuffer[(String, NodeInput)]()
  val relationsInput = ArrayBuffer[(String, RelationshipInput)]()

  val n1 = TestNode(TestId(1), Seq(LynxNodeLabel("Person")), Map(LynxPropertyKey("name")-> LynxValue("A")))
  val n2 = TestNode(TestId(2), Seq(LynxNodeLabel("Person")), Map(LynxPropertyKey("name")-> LynxValue("B")))


  @BeforeEach
  def init(): Unit ={
    nodesInput.append(("n1", NodeInput(n1.labels, n1.props.toSeq)))
    nodesInput.append(("n2", NodeInput(n2.labels, n2.props.toSeq)))

    model.write.createElements(nodesInput, relationsInput,
      (nodesCreated: Seq[(String, LynxNode)], relsCreated: Seq[(String, LynxRelationship)]) => {
        nodesCreated.toMap ++ relsCreated
      }
    )
    model.write.commit
  }

  @Test
  def createSingleNode(): Unit ={
    val num = nodesInput.length
    runOnDemoGraph(
      """
        |create (n)
        |""".stripMargin)

    Assertions.assertEquals(num + 1, all_nodes.size)
  }

  @Test
  def createMultipleNodes(): Unit ={
    val num = nodesInput.length
    runOnDemoGraph(
      """
        |CREATE (n), (m)
        |""".stripMargin)

    Assertions.assertEquals(num + 2, all_nodes.size)
  }

  @Test
  def CreateANodeWithALabel(): Unit ={
    val num = nodesInput.length
    runOnDemoGraph(
      """
        |CREATE (n:Person)
        |""".stripMargin)
    Assertions.assertEquals(num + 1, all_nodes.size)
  }

  @Test
  def CreateNodeWithMultipleLabel(): Unit ={
    val num = nodesInput.length
    runOnDemoGraph(
      """
        |CREATE (n:Person:Swedish)
        |""".stripMargin)
    Assertions.assertEquals(num + 1, all_nodes.size)
  }

  @Test
  def CreateNodeAndAddLabelsAndProperty(): Unit ={
    val num = nodesInput.length
    runOnDemoGraph(
      """
        |CREATE (n:Person {name: 'Andy', title: 'Developer'})
        |""".stripMargin)
    Assertions.assertEquals(num + 1, all_nodes.size)
  }

  @Test
  def returnCreatedNode(): Unit ={
    val num = nodesInput.length

    val res = runOnDemoGraph(
      """
        |CREATE (a {name: 'Andy'})
        |RETURN a.name
        |""".stripMargin).records().toArray
    Assertions.assertEquals(num + 1, all_nodes.size)
    Assertions.assertEquals("Andy", res(0)("a.name").asInstanceOf[LynxValue].value)
  }

  @Test
  def createRelationshipBetweenTwoNodes(): Unit ={
    val res = runOnDemoGraph(
      """
        |MATCH
        |  (a:Person),
        |  (b:Person)
        |WHERE a.name = 'A' AND b.name = 'B'
        |CREATE (a)-[r:RELTYPE]->(b)
        |RETURN type(r)
        |""".stripMargin).records().toArray

    Assertions.assertEquals("RELTYPE", res(0)("type(r)").asInstanceOf[LynxValue].value)
  }

  @Test
  def createRelationshipAndSetProperty(): Unit ={
    val res = runOnDemoGraph(
      """
        |MATCH
        |  (a:Person),
        |  (b:Person)
        |WHERE a.name = 'A' AND b.name = 'B'
        |CREATE (a)-[r:RELTYPE {name: a.name + '<->' + b.name}]->(b)
        |RETURN type(r), r.name
        |""".stripMargin).records().toArray

    Assertions.assertEquals("RELTYPE", res(0)("type(r)").asInstanceOf[LynxValue].value)
    Assertions.assertEquals("A<->B", res(0)("r.name").asInstanceOf[LynxValue].value)
  }

  @Test
  def createFullPath(): Unit ={
    val numNode = nodesInput.length
    val numRels = relationsInput.length
    val res = runOnDemoGraph(
      """
        |CREATE p = (andy {name:'Andy'})-[:WORKS_AT]->(neo)<-[:WORKS_AT]-(michael {name: 'Michael'})
        |RETURN p
        |""".stripMargin).records().toArray
    Assertions.assertEquals(numNode + 3, all_nodes.size)
    Assertions.assertEquals(numRels + 2, all_rels.size)
  }

  @Test
  def createNodeWithAParameterForTheProperties(): Unit = {
    val num = nodesInput.length
    runOnDemoGraph(
      """
        |CREATE (n:Person $props)
        |RETURN n
        |""".stripMargin, Map("props" -> Map(
        "name" -> "Andy",
        "position" -> "Developer"
      )))
    Assertions.assertEquals(num + 1, all_nodes.size)
  }

  @Test
  def createMultipleNodeWithAParameterForTheProperties(): Unit = {
    val numNode = nodesInput.length
    val numRels = relationsInput.length
    runOnDemoGraph(
      """
        |UNWIND $props AS map
        |CREATE (n)
        |SET n = map
        |""".stripMargin, Map("props" -> List(
          Map(
            "name"-> "Andy",
            "position"-> "Developer"
          ), Map(
            "name"-> "Michael",
            "position"-> "Developer"
        ))))
    Assertions.assertEquals(numNode + 2, all_nodes.size)
  }
}
