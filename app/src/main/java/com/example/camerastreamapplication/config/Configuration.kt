package com.example.camerastreamapplication.config

//NN configuration
const val GRID_WIDTH = 13
const val GRID_HEIGHT = 13
const val BOXES_PER_SEGMENT = 5
const val NUM_OF_CLASSES = 80
const val INPUT_WIDTH = 416
const val INPUT_HEIGHT = 416

const val CELL_WIDTH = 32
const val CELL_HEIGHT = 32

const val OFFSET_PER_BOX = NUM_OF_CLASSES + 5

val ANCHORS = arrayOf(0.57273f, 0.677385f, 1.87446f, 2.06253f, 3.33843f, 5.47434f, 7.88282f, 3.52778f, 9.77052f, 9.16828f)

var BOX_DETECTION_THRESHOLD = 0.55f
var CLASS_CONFIDENCE_THRESHOLD = 0.6f


var IoU_THRESHOLD = 0.4f

//Audio notificator configuration

var MAX_OBJECT_NOTIFICATIONS = 3

//Shared Preferences
const val SHARED_PREFERENCES_NAME = "Configuration"
const val BOX_DETECTION_KEY = "BoxDetection"
const val CLASS_DETECTION_KEY = "ClassDetection"
const val IOU_THRESHOLD_KEY = "IouThreshold"
const val MAX_NOTIFICATIONS_KEY = "MaxNotifications"
