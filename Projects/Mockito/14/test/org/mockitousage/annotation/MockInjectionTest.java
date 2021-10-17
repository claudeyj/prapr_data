/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.annotation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.exceptions.base.MockitoException;
import org.mockitoutil.TestBase;

import java.util.*;

@SuppressWarnings({"unchecked", "unused"})
public class MockInjectionTest extends TestBase {

	private SuperUnderTesting superUnderTestWithoutInjection = new SuperUnderTesting();
	@InjectMocks private SuperUnderTesting superUnderTest = new SuperUnderTesting();
	@InjectMocks private BaseUnderTesting baseUnderTest = new BaseUnderTesting();
	@InjectMocks private SubUnderTesting subUnderTest = new SubUnderTesting();
	@InjectMocks private OtherBaseUnderTesting otherBaseUnderTest = new OtherBaseUnderTesting();
	@Mock private Map map;	
    @Mock private List list;
	@Mock private Set histogram1;
	@Mock private Set histogram2;
	@Spy private TreeSet searchTree = new TreeSet();

	@Before
	public void init() {
		// initMocks called in TestBase Before method, so instances ar not the same
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldInjectMocksIfAnnotated() {
		MockitoAnnotations.initMocks(this);
		assertSame(list, superUnderTest.getAList());
	}

	@Test
	public void shouldNotInjectIfNotAnnotated() {
		MockitoAnnotations.initMocks(this);
		assertNull(superUnderTestWithoutInjection.getAList());
	}

	@Test
	public void shouldInjectMocksForClassHierarchyIfAnnotated() {
		MockitoAnnotations.initMocks(this);
		assertSame(list, baseUnderTest.getAList());
		assertSame(map, baseUnderTest.getAMap());
	}

	@Test
	public void shouldInjectMocksByName() {
		MockitoAnnotations.initMocks(this);
		assertSame(histogram1, subUnderTest.getHistogram1());
		assertSame(histogram2, subUnderTest.getHistogram2());
	}

	@Test
	public void shouldInjectSpies() {
		MockitoAnnotations.initMocks(this);
		assertSame(searchTree, otherBaseUnderTest.getSearchTree());
	}
	
    @Test(expected=MockitoException.class)
    public void shouldProvideDecentExceptionWhenInjectMockInstanceIsNull() throws Exception {
        MockitoAnnotations.initMocks(new Object() {
           @InjectMocks Object iAmNull = null; 
        });
    }

	class SuperUnderTesting {

		private List aList;

		public List getAList() {
			return aList;
		}
	}

	class BaseUnderTesting extends SuperUnderTesting {
		private Map aMap;

		public Map getAMap() {
			return aMap;
		}
	}

	class OtherBaseUnderTesting extends SuperUnderTesting {
		private TreeSet searchTree;

		public TreeSet getSearchTree() {
			return searchTree;
		}
	}

	class SubUnderTesting extends BaseUnderTesting {
		private Set histogram1;
		private Set histogram2;

		public Set getHistogram1() {
			return histogram1;
		}

		public Set getHistogram2() {
			return histogram2;
		}
	}
}
