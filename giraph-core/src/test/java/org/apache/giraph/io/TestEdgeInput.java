/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.io;

import org.apache.giraph.BspCase;
import org.apache.giraph.conf.GiraphConfiguration;
import org.apache.giraph.conf.ImmutableClassesGiraphConfiguration;
import org.apache.giraph.edge.ByteArrayEdges;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.VertexValueFactory;
import org.apache.giraph.io.formats.IdWithValueTextOutputFormat;
import org.apache.giraph.io.formats.IntIntTextVertexValueInputFormat;
import org.apache.giraph.io.formats.IntNullReverseTextEdgeInputFormat;
import org.apache.giraph.io.formats.IntNullTextEdgeInputFormat;
import org.apache.giraph.utils.InternalVertexRunner;
import org.apache.giraph.vertices.IntIntNullVertexDoNothing;
import org.apache.giraph.vertices.VertexCountEdges;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.junit.Test;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A test case to ensure that loading a graph from a list of edges works as
 * expected.
 */
public class TestEdgeInput extends BspCase {
  public TestEdgeInput() {
    super(TestEdgeInput.class.getName());
  }

  // It should be able to build a graph starting from the edges only.
  // Vertices should be implicitly created with default values.
  @Test
  public void testEdgesOnly() throws Exception {
    String[] edges = new String[] {
        "1 2",
        "2 3",
        "2 4",
        "4 1"
    };

    GiraphConfiguration conf = new GiraphConfiguration();
    conf.setVertexClass(VertexCountEdges.class);
    conf.setOutEdgesClass(ByteArrayEdges.class);
    conf.setEdgeInputFormatClass(IntNullTextEdgeInputFormat.class);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Iterable<String> results = InternalVertexRunner.run(conf, null, edges);

    Map<Integer, Integer> values = parseResults(results);

    // Check that all vertices with outgoing edges have been created
    assertEquals(3, values.size());
    // Check the number of edges for each vertex
    assertEquals(1, (int) values.get(1));
    assertEquals(2, (int) values.get(2));
    assertEquals(1, (int) values.get(4));
  }

  // It should be able to build a graph starting from the edges only.
  // Using ReverseEdgeDuplicator it should also create the reverse edges.
  // Vertices should be implicitly created with default values.
  @Test
  public void testEdgesOnlyWithReverse() throws Exception {
    String[] edges = new String[] {
        "1 2",
        "2 3",
        "2 4",
        "4 1"
    };

    GiraphConfiguration conf = new GiraphConfiguration();
    conf.setVertexClass(VertexCountEdges.class);
    conf.setOutEdgesClass(ByteArrayEdges.class);
    conf.setEdgeInputFormatClass(IntNullReverseTextEdgeInputFormat.class);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Iterable<String> results = InternalVertexRunner.run(conf, null, edges);

    Map<Integer, Integer> values = parseResults(results);

    // Check that all vertices with outgoing edges have been created
    assertEquals(4, values.size());
    // Check the number of edges for each vertex
    assertEquals(2, (int) values.get(1));
    assertEquals(3, (int) values.get(2));
    assertEquals(1, (int) values.get(3));
    assertEquals(2, (int) values.get(4));
  }

  // It should be able to build a graph by specifying vertex data and edges
  // as separate input formats.
  @Test
  public void testMixedFormat() throws Exception {
    String[] vertices = new String[] {
        "1 75",
        "2 34",
        "3 13",
        "4 32"
    };
    String[] edges = new String[] {
        "1 2",
        "2 3",
        "2 4",
        "4 1",
        "5 3"
    };

    GiraphConfiguration conf = new GiraphConfiguration();
    conf.setVertexClass(IntIntNullVertexDoNothing.class);
    conf.setOutEdgesClass(ByteArrayEdges.class);
    conf.setVertexInputFormatClass(IntIntTextVertexValueInputFormat.class);
    conf.setEdgeInputFormatClass(IntNullTextEdgeInputFormat.class);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);

    // Run a job with a vertex that does nothing
    Iterable<String> results = InternalVertexRunner.run(conf, vertices, edges);

    Map<Integer, Integer> values = parseResults(results);

    // Check that all vertices with either initial values or outgoing edges
    // have been created
    assertEquals(5, values.size());
    // Check that the vertices have been created with correct values
    assertEquals(75, (int) values.get(1));
    assertEquals(34, (int) values.get(2));
    assertEquals(13, (int) values.get(3));
    assertEquals(32, (int) values.get(4));
    // A vertex with edges but no initial value should have the default value
    assertEquals(0, (int) values.get(5));

    // Run a job with a custom VertexValueFactory
    conf.setVertexValueFactoryClass(TestVertexValueFactory.class);
    results = InternalVertexRunner.run(conf, vertices, edges);
    values = parseResults(results);
    // A vertex with edges but no initial value should have been constructed
    // by the custom factory
    assertEquals(3, (int) values.get(5));

    conf = new GiraphConfiguration();
    conf.setVertexClass(VertexCountEdges.class);
    conf.setOutEdgesClass(ByteArrayEdges.class);
    conf.setVertexInputFormatClass(IntIntTextVertexValueInputFormat.class);
    conf.setEdgeInputFormatClass(IntNullTextEdgeInputFormat.class);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);

    // Run a job with a vertex that counts outgoing edges
    results = InternalVertexRunner.run(conf, vertices, edges);

    values = parseResults(results);

    // Check the number of edges for each vertex
    assertEquals(1, (int) values.get(1));
    assertEquals(2, (int) values.get(2));
    assertEquals(0, (int) values.get(3));
    assertEquals(1, (int) values.get(4));
    assertEquals(1, (int) values.get(5));
  }

  // It should use the specified input OutEdges class.
  @Test
  public void testDifferentInputEdgesClass() throws Exception {
    String[] edges = new String[] {
        "1 2",
        "2 3",
        "2 4",
        "4 1"
    };

    GiraphConfiguration conf = new GiraphConfiguration();
    conf.setVertexClass(TestVertexCheckEdgesType.class);
    conf.setOutEdgesClass(ByteArrayEdges.class);
    conf.setInputOutEdgesClass(TestOutEdgesFilterEven.class);
    conf.setEdgeInputFormatClass(IntNullTextEdgeInputFormat.class);
    conf.setVertexOutputFormatClass(IdWithValueTextOutputFormat.class);
    Iterable<String> results = InternalVertexRunner.run(conf, null, edges);

    Map<Integer, Integer> values = parseResults(results);

    // Check that all vertices with outgoing edges in the input have been
    // created
    assertEquals(3, values.size());
    // Check the number of edges for each vertex (edges with odd target id
    // should have been removed)
    assertEquals(1, (int) values.get(1));
    assertEquals(1, (int) values.get(2));
    assertEquals(0, (int) values.get(4));
  }

  public static class TestVertexCheckEdgesType extends VertexCountEdges {
    @Override
    public void compute(Iterable<NullWritable> messages) throws IOException {
      assertFalse(getEdges() instanceof TestOutEdgesFilterEven);
      assertTrue(getEdges() instanceof ByteArrayEdges);
      super.compute(messages);
    }
  }

  public static class TestVertexValueFactory
      implements VertexValueFactory<IntWritable> {
    @Override
    public void initialize(ImmutableClassesGiraphConfiguration<?, IntWritable,
            ?, ?> configuration) { }

    @Override
    public IntWritable createVertexValue() {
      return new IntWritable(3);
    }
  }

  public static class TestOutEdgesFilterEven
      extends ByteArrayEdges<IntWritable, NullWritable> {
    @Override
    public void add(Edge<IntWritable, NullWritable> edge) {
      if (edge.getTargetVertexId().get() % 2 == 0) {
        super.add(edge);
      }
    }
  }

  private static Map<Integer, Integer> parseResults(Iterable<String> results) {
    Map<Integer, Integer> values = Maps.newHashMap();
    for (String line : results) {
      String[] tokens = line.split("\\s+");
      int id = Integer.valueOf(tokens[0]);
      int value = Integer.valueOf(tokens[1]);
      values.put(id, value);
    }
    return values;
  }
}
