package com.example.camerastreamapplication.tfLiteWrapper

const val GRID_WIDTH = 13
const val GRID_HEIGHT = 13
const val BOXES_PER_SEGMENT = 5
const val NUM_OF_CLASSES = 80

const val DETECTION_THRESHOLD = 0.4

const val OFFSET_PER_SEGMENT = (BOXES_PER_SEGMENT * (NUM_OF_CLASSES + 5))
const val OFFSET_PER_BOX = NUM_OF_CLASSES + 5
const val CONFIDENCE_OFFSET = 4
const val CLASS_DATA_OFFSET = 5

