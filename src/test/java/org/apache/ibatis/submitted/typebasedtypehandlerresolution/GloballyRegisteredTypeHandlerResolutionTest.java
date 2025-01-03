/*
 *    Copyright 2009-2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.submitted.typebasedtypehandlerresolution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GloballyRegisteredTypeHandlerResolutionTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setUp() throws Exception {
    Environment environment = new Environment("test", new JdbcTransactionFactory(),
        new UnpooledDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:globalthresolution", null));
    Configuration configuration = new Configuration();
    configuration.setEnvironment(environment);
    // Register against different Types
    configuration.getTypeHandlerRegistry().register(new TypeReference<FuzzyBean<?>>() {
    }.getRawType(), TypeAwareTypeHandler.class);
    configuration.getTypeHandlerRegistry().register(new TypeReference<List<String>>() {
    }.getRawType(), CsvTypeHandler.class);
    configuration.getTypeHandlerRegistry().register(new TypeReference<List<Integer>>() {
    }.getRawType(), CsvTypeHandler.class);
    configuration.addMapper(GloballyRegisteredHandlerMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    BaseDataTest.runScript(configuration.getEnvironment().getDataSource(),
        "org/apache/ibatis/submitted/typebasedtypehandlerresolution/CreateDB.sql");
  }

  @Test
  void handlerResolutionInResultAnnotation() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      GloballyRegisteredHandlerMapper mapper = sqlSession.getMapper(GloballyRegisteredHandlerMapper.class);
      User user = mapper.getUser(1);
      assertEquals("garden", user.getStrvalue().getValue());
      assertEquals(31, user.getIntvalue().getValue());
      assertEquals(Arrays.asList("a", "b", "c"), user.getStrings());
      assertEquals(Arrays.asList(1, 3, 5), user.getIntegers());
    }
  }

  @Test
  void handlerResolutionInParameterMapping() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      GloballyRegisteredHandlerMapper mapper = sqlSession.getMapper(GloballyRegisteredHandlerMapper.class);
      {
        User user = new User();
        user.setId(12);
        user.setStrvalue(new FuzzyBean<String>("park"));
        user.setIntvalue(new FuzzyBean<Integer>(7));
        user.setStrings(Arrays.asList("aa", "bb"));
        user.setIntegers(Arrays.asList(11, 22));
        mapper.insertUser(user);
      }
      {
        User user = mapper.getUser(12);
        assertEquals("park", user.getStrvalue().getValue());
        assertEquals(7, user.getIntvalue().getValue());
        assertEquals(Arrays.asList("aa", "bb"), user.getStrings());
        assertEquals(Arrays.asList(11, 22), user.getIntegers());
      }
    }
  }

  @Test
  void handlerResolutionInParameterMapping_MultiParams() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      GloballyRegisteredHandlerMapper mapper = sqlSession.getMapper(GloballyRegisteredHandlerMapper.class);
      {
        User user = new User();
        user.setId(13);
        user.setStrvalue(new FuzzyBean<String>("well"));
        user.setIntvalue(new FuzzyBean<Integer>(23));
        user.setStrings(Arrays.asList("aa", "bb"));
        user.setIntegers(Arrays.asList(11, 22));
        mapper.insertUserMultiParam(user, "whatevs");
      }
      {
        User user = mapper.getUser(13);
        assertEquals("well", user.getStrvalue().getValue());
        assertEquals(23, user.getIntvalue().getValue());
        assertEquals(Arrays.asList("aa", "bb"), user.getStrings());
        assertEquals(Arrays.asList(11, 22), user.getIntegers());
      }
    }
  }

  @Test
  void handlerResolutionInXmlResultMap() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      User user = sqlSession.selectOne(
          "org.apache.ibatis.submitted.typebasedtypehandlerresolution.GloballyRegisteredHandlerMapper.selectXml", 1);
      assertEquals("garden", user.getStrvalue().getValue());
      assertEquals(31, user.getIntvalue().getValue());
      assertEquals(Arrays.asList("a", "b", "c"), user.getStrings());
      assertEquals(Arrays.asList(1, 3, 5), user.getIntegers());
    }
  }

  @Test
  void handlerResolutionInXmlParameter() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      {
        User user = new User();
        user.setId(13);
        user.setStrvalue(new FuzzyBean<String>("pond"));
        user.setIntvalue(new FuzzyBean<Integer>(23));
        user.setStrings(Arrays.asList("aa", "bb"));
        user.setIntegers(Arrays.asList(11, 22));
        sqlSession.insert(
            "org.apache.ibatis.submitted.typebasedtypehandlerresolution.GloballyRegisteredHandlerMapper.insertXml",
            user);
      }
      {
        GloballyRegisteredHandlerMapper mapper = sqlSession.getMapper(GloballyRegisteredHandlerMapper.class);
        User user = mapper.getUser(13);
        assertEquals("pond", user.getStrvalue().getValue());
        assertEquals(23, user.getIntvalue().getValue());
        assertEquals(Arrays.asList("aa", "bb"), user.getStrings());
        assertEquals(Arrays.asList(11, 22), user.getIntegers());
      }
    }
  }

  @Test
  void handlerResolutionInXmlParameterWithoutParameterType() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      {
        User user = new User();
        user.setId(14);
        user.setStrvalue(new FuzzyBean<String>("library"));
        user.setIntvalue(new FuzzyBean<Integer>(38));
        user.setStrings(Arrays.asList("aa", "bb"));
        user.setIntegers(Arrays.asList(11, 22));
        sqlSession.insert(
            "org.apache.ibatis.submitted.typebasedtypehandlerresolution.GloballyRegisteredHandlerMapper.insertXmlWithoutParameterType",
            user);
      }
      {
        GloballyRegisteredHandlerMapper mapper = sqlSession.getMapper(GloballyRegisteredHandlerMapper.class);
        User user = mapper.getUser(14);
        assertEquals("library", user.getStrvalue().getValue());
        assertEquals(38, user.getIntvalue().getValue());
        assertEquals(Arrays.asList("aa", "bb"), user.getStrings());
        assertEquals(Arrays.asList(11, 22), user.getIntegers());
      }
    }
  }

  @Test
  void handleReturnTypeWithTypeParam() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      GloballyRegisteredHandlerMapper mapper = sqlSession.getMapper(GloballyRegisteredHandlerMapper.class);
      FuzzyBean<String> fuzzyBean = mapper.selectFuzzyString(1);
      assertEquals("garden", fuzzyBean.getValue());
    }
  }
}
