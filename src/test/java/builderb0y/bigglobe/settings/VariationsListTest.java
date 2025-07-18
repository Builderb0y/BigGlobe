package builderb0y.bigglobe.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.DataOps;
import builderb0y.autocodec.data.ListData;
import builderb0y.autocodec.data.UnknownData;

import static org.junit.jupiter.api.Assertions.*;

//language=json

public class VariationsListTest {

	@Test
	public void testDefaulted() {
		Data input = data(
			"""
			{
				"defaults": { "a": 1 },
				"variations": [
					{ "b": 1 },
					{ "b": 2 },
					{ "b": 3 }
				]
			}
			"""
		);
		Data expected = data(
			"""
			[
				{ "a": 1, "b": 1 },
				{ "a": 1, "b": 2 },
				{ "a": 1, "b": 3 }
			]
			"""
		);
		Data actual = ListData.collect(VariationsList.expand(input));
		assertEquals(expected, actual);
	}

	@Test
	public void testLayered() {
		Data input = data(
			"""
			{
				"variations": [
					[ { "a": 1 }, { "a": 2 } ],
					[ { "b": 1 }, { "b": 2 } ],
					[ { "c": 1 }, { "c": 2 } ]
				]
			}
			"""
		);
		Data expected = data(
			"""
			[
				{ "a": 1, "b": 1, "c": 1 },
				{ "a": 1, "b": 1, "c": 2 },
				{ "a": 1, "b": 2, "c": 1 },
				{ "a": 1, "b": 2, "c": 2 },
				{ "a": 2, "b": 1, "c": 1 },
				{ "a": 2, "b": 1, "c": 2 },
				{ "a": 2, "b": 2, "c": 1 },
				{ "a": 2, "b": 2, "c": 2 }
			]
			"""
		);
		Data actual = ListData.collect(VariationsList.expand(input));
		assertEquals(expected, actual);
	}

	@Test
	public void testDeep() {
		Data input = data(
			"""
			{
				"defaults": { "a": { "b": { "c": 1 } } },
				"variations": [
					{ "a": { "b": { "d": 1 } } },
					{ "a": { "b": { "d": 2 } } }
				],
				"deep": true
			}
			"""
		);
		Data expected = data(
			"""
			[
				{ "a": { "b": { "c": 1, "d": 1 } } },
				{ "a": { "b": { "c": 1, "d": 2 } } }
			]
			"""
		);
		Data actual = ListData.collect(VariationsList.expand(input));
		assertEquals(expected, actual);
	}

	@Test
	public void testNotDeep() {
		Data input = data(
			"""
			{
				"defaults": { "a": { "b": { "c": 1 } } },
				"variations": [
					{ "a": { "b": { "d": 1 } } },
					{ "a": { "b": { "d": 2 } } }
				],
				"deep": false
			}
			"""
		);
		Data expected = data(
			"""
			[
				{ "a": { "b": { "d": 1 } } },
				{ "a": { "b": { "d": 2 } } }
			]
			"""
		);
		Data actual = ListData.collect(VariationsList.expand(input));
		assertEquals(expected, actual);
	}

	@Test
	public void testNested() {
		Data input = data(
			"""
			{
				"defaults": { "a": 1 },
				"variations": [
					{
						"defaults": { "b": 1 },
						"variations": [
							{ "c": 1 },
							{ "c": 2 }
						]
					},
					{
						"defaults": { "b": 2 },
						"variations": [
							{ "c": 1 },
							{ "c": 2 }
						]
					}
				]
			}
			"""
		);
		Data expected = data(
			"""
			[
				{ "a": 1, "b": 1, "c": 1 },
				{ "a": 1, "b": 1, "c": 2 },
				{ "a": 1, "b": 2, "c": 1 },
				{ "a": 1, "b": 2, "c": 2 }
			]
			"""
		);
		Data actual = ListData.collect(VariationsList.expand(input));
		assertEquals(expected, actual);
	}

	public static Data data(String jsonText) {
		return new UnknownData<>(JsonOps.INSTANCE, JsonParser.parseString(jsonText));
	}
}