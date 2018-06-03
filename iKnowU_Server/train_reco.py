"""
# train_reco.py
# Author: Moreno Colombo
# Created on: 05.12.2017
#
# Training of face recognition using dlib
#
# Algorithm:
#
# - Go through all of the folders inside training_images (each folder corresponding to one person, where the folder's name = person's name)
# - Find face descriptors for each image inside each folder
# - Save in a file all the registered person's names and the corresponding face descriptors
#
"""

import dlib
import numpy as np
from skimage import io
from os import listdir
from os import path

training_path = "training_images"
'''
# Load all the models we need: a face detector, a shape predictor for face landmarks,
# and the face recognition model (using distances between face landmarks)
'''
detector = dlib.get_frontal_face_detector()
sp = dlib.shape_predictor("shape_predictor_68_face_landmarks.dat")
facerec = dlib.face_recognition_model_v1("dlib_face_recognition_resnet_model_v1.dat")

trainingVects, trainingLabels = [], [] 					# Initialize result vectors

# Go through all folders (named each after a person), all images inside them and process those 
for fn in listdir("training_images"):
	try:
		for fi in listdir(path.join("training_images",fn)):
			img = io.imread(path.join("training_images", fn, fi)) # Read image
			dets = detector(img, 1)							# Find face in image
	
			# Process each found face, and put the found descriptors in a list to be saved
			for k, d in enumerate(dets):
				trainingVects.append(facerec.compute_face_descriptor(img, sp(img, d)))
				trainingLabels.append(fn)
	except:
		pass

# Save training data
np.save("trainingVects.npy", trainingVects)
np.save("trainingLabels.npy", trainingLabels)