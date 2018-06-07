"""
# facereco.py
# Author: Moreno Colombo
# Created on: 05.12.2017
#
# Face recognition on images coming from external device
#
# Algorithm:
#
# 1. Start socket connection
# 2. Receive image from external device via socket
# 3. Preprocess image to increase face descriptors efficiency and accuracy
# 4. Find face descriptors for each face in the image
# 5. Associate the faces to the best match from the training set if distance is below the threshold
# 6. If image matches the searched person (press "c" to randomly change the searched person), then signal it
# 6. Send result back to external device (position of all faces and wether they belong to the searched person or not)
# 7. Repeat from point 2. until user doesn't press "q"
"""
import socket
import sys
import io
import numpy as np
import cv2
import time
import dlib
from sklearn import neighbors
import pygame
import random
import os

ipAddress = "192.168.1.103" # Define IP address according to IP address of external device

'''
# Load all the models we need: a face detector, a shape predictor for face landmarks,
# and the face recognition model (using distances between face landmarks)
'''
detector = dlib.get_frontal_face_detector()
predictor = dlib.shape_predictor("shape_predictor_68_face_landmarks.dat")
facerec = dlib.face_recognition_model_v1("dlib_face_recognition_resnet_model_v1.dat")

# Load trained face descriptors, and put them in a tree to find the closest one faster
trainingVects = np.load("trainingVects.npy")
trainingLabels = np.load("trainingLabels.npy")
tree = neighbors.BallTree(trainingVects, leaf_size=2)

loop = True										# True until one presses "q"
searchedPerson = 0								# Index of the person to be searched
ratio = 1.2										# Ratio used to resize received image

'''
# Function to display result on pc
'''
def display_result(frame, res):
	if len(res)>0:
		for i in range((len(res)//5)):
			l,t,r,b = int(res[i*5+1]/ratio), int(res[i*5+2]/ratio), int(res[i*5+3]/ratio), int(res[i*5+4]/ratio)
			if len(res[i*5])>1 and res[i*5] == trainingLabels[searchedPerson]:
				cv2.rectangle(frame, (l,t),(r,b),(0,255,0),3)
				cv2.putText(frame,res[i*5],(l,b+20), cv2.FONT_HERSHEY_SIMPLEX, 2,(255,255,255),2,cv2.LINE_AA)
			else:
				cv2.rectangle(frame, (l,t),(r,b),(0,0,255),3)
					
	cv2.namedWindow("Video")        			# Create a named window
	cv2.moveWindow("Video", 250,100)  			# Move it
	cv2.imshow("Video", frame)
	cv2.waitKey(1)
	return 0
'''
# Function to display image of person we are searching
'''
def display_thumbnail(name):
	imname = os.listdir(os.path.join("training_images",name))[0]
	im = cv2.imread(os.path.join("training_images", name, imname))
	cv2.namedWindow("SearchedPerson")        	# Create a named window
	cv2.moveWindow("SearchedPerson", 800,100)	# Move it to (800,100)
	cv2.imshow("SearchedPerson", im)
	return 0

'''
# Function to recognize person from a picture
'''
def recognize_people(frame):
	# Preprocessing of frame to increase precision and to speed things up
	frame = cv2.resize(frame, (0,0), fx=1/ratio, fy= 1/ratio) #533,400
	gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
	clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
	clahe_image = clahe.apply(gray)

	detections = detector(clahe_image, 1) 		#Detect the faces in the image
	results = []
	
	for k in range(0,len(detections)):
		face = detections[k]
		# Compute descriptors for each face
		face_descriptor = facerec.compute_face_descriptor(frame, predictor(frame, face))
		
		# Find closest face descriptor from training set
		dist, ind = tree.query([face_descriptor], k=1)
		pred_face = trainingLabels[ind[0][0]] if dist[0][0]<0.65 else ""
		
		# Result element: (Person name, left, top, right, bottom)		
		results += [pred_face, int(face.left()*ratio),int(face.top()*ratio),int(face.right()*ratio),int(face.bottom()*ratio)]
	
	display_result(frame, results)				# Display image with overlays
	return results
		
# Create a pygame window, to check for keyboard inputs
pygame.init()
windowSurface = pygame.display.set_mode((200, 200), pygame.RESIZABLE, 32)
windowSurface.fill((0,0,0))		
		
# Create a TCP/IP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Bind the socket to the port
server_address = (ipAddress, 9002)
print('starting up on %s port %s' % server_address)
sock.bind(server_address)

# Listen for incoming connections
sock.listen(1)

while loop:
	# Handle button presses
	for event in pygame.event.get():
		if event.type == pygame.KEYDOWN: 
			if event.key == pygame.K_q:
				loop = False
				print("Exit")
				sys.exit()
			if event.key == pygame.K_c:
				searchedPerson = random.randrange(0,len(trainingLabels))
				display_thumbnail(trainingLabels[searchedPerson])
				print("Now looking for: "+trainingLabels[searchedPerson])
				
	# Wait for an incoming connection
	connection, client_address = sock.accept()
	try:
		error = False
		buff = io.BytesIO()
		start = time.time()
		#Receive image size
		d = connection.recv(8)
		id = d.find(b'\xff')
		if id!=-1:
			size = int(d[0:id])
		else:
			size = int(d)
		received = 0

		# Receive the data in small chunks (128B)
		while received<size:
			data = connection.recv(128)#4096)#65536)
			received += len(data)
			if data:
				buff.write(data)
			else:
				error = True
				break
		res = ""
		if not error:
			buff.seek(0)
			nparr = np.fromstring(buff.getvalue(), np.uint8)
			if nparr.size == size:
				img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
												# Decode image
				res = ",".join(map(str,recognize_people(img)))
												# Recognize people and put the in a string and send back to external device
		connection.send((res).encode()) 
	except Exception as e:
		connection.send("".encode())
	finally:
		# Clean up the connection
		connection.close()