int*(x = floorInt(listener.eyeX), z = floorInt(listener.eyeZ))
if (`bigglobe:nether/bubble_type`(x, z) == NetherBubble.VALLEY_OF_SOULS:
	return('bigglobe:reverb_small')
)
return('bigglobe:reverb_nether')