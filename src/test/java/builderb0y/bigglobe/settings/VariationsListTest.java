package builderb0y.bigglobe.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//language=json

public class VariationsListTest {

	@Test
	public void testDefaulted() {
		JsonElement input = JsonParser.parseString(
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
		JsonElement expected = JsonParser.parseString(
			"""
			[
				{ "a": 1, "b": 1 },
				{ "a": 1, "b": 2 },
				{ "a": 1, "b": 3 }
			]
			"""
		);
		JsonElement actual = JsonOps.INSTANCE.createList(VariationsList.expand(input, JsonOps.INSTANCE));
		assertEquals(expected, actual);
	}

	@Test
	public void testLayered() {
		JsonElement input = JsonParser.parseString(
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
		JsonElement expected = JsonParser.parseString(
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
		JsonElement actual = JsonOps.INSTANCE.createList(VariationsList.expand(input, JsonOps.INSTANCE));
		assertEquals(expected, actual);
	}

	@Test
	public void testDeep() {
		JsonElement input = JsonParser.parseString(
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
		JsonElement expected = JsonParser.parseString(
			"""
			[
				{ "a": { "b": { "c": 1, "d": 1 } } },
				{ "a": { "b": { "c": 1, "d": 2 } } }
			]
			"""
		);
		JsonElement actual = JsonOps.INSTANCE.createList(VariationsList.expand(input, JsonOps.INSTANCE));
		assertEquals(expected, actual);
	}

	@Test
	public void testNotDeep() {
		JsonElement input = JsonParser.parseString(
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
		JsonElement expected = JsonParser.parseString(
			"""
			[
				{ "a": { "b": { "d": 1 } } },
				{ "a": { "b": { "d": 2 } } }
			]
			"""
		);
		JsonElement actual = JsonOps.INSTANCE.createList(VariationsList.expand(input, JsonOps.INSTANCE));
		assertEquals(expected, actual);
	}

	@Test
	public void testNested() {
		JsonElement input = JsonParser.parseString(
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
		JsonElement expected = JsonParser.parseString(
			"""
			[
				{ "a": 1, "b": 1, "c": 1 },
				{ "a": 1, "b": 1, "c": 2 },
				{ "a": 1, "b": 2, "c": 1 },
				{ "a": 1, "b": 2, "c": 2 }
			]
			"""
		);
		JsonElement actual = JsonOps.INSTANCE.createList(VariationsList.expand(input, JsonOps.INSTANCE));
		assertEquals(expected, actual);
	}
}