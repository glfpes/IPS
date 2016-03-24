#!/usr/bin/env python
# vim: sts=4 ts=4 sw=4 noet:

from __future__ import print_function
import sys, os
import argparse
import string
import numpy
import numpy.ma
import scipy
import scipy.misc
import scipy.ndimage
import scipy.signal
import scipy.cluster
import matplotlib
import matplotlib.mlab


from aoa import aoa

import pretty_logger
logger = pretty_logger.get_logger()

def dist(c1, c2):
	return ( (c1[0] - c2[0])**2 + (c1[1] - c2[1])**2 - (c1[2] - c2[2])**2)**.5

def get_light():
	vlccpp_pair = "C:\\Users\\glfpes\\Desktop\\VLC\\trunk\\vlc\\Debug\\pairs.txt";
	file_object = open(vlccpp_pair);
	cols=2
	rows=5
	image = [[0 for col in range(cols)] for row in range(rows)];
	cols=3
	rows=5
	room =  [[0 for col in range(cols)] for row in range(rows)];
	i=0
	try:
		for line in file_object:
			j=i/5;
			k=i%5;
			number = string.atof(line);
			if(k<2):
				image[j][k]=number
			if(k>1):
				room[j][k-2]=number
			i=i+1;
		lights = [
			(
				image[i],
				(room[i],)
			) for i in xrange(len(image))]
		return lights
	finally:
		file_object.close();

	
def resolve_aliased_frequncies(lights):
	l,p = zip(*lights)
	def build_opts(s, p, r):
		if len(p) == 1:
			for e in p[0]:
				new_s = list(s)
				new_s.append(e)
				r.append(new_s)
		else:
			for i in range(len(p[0])):
				sub_s = list(s)
				sub_s.append(p[0][i])
				sub_p = list(p)
				sub_p.pop(0)
				build_opts(sub_s, sub_p, r)
	r = []
	build_opts([], p, r)

	best_dist = 1e12
	for coords in r:
		d = 0
		for i in range(len(coords)-1):
			for j in range(1, len(coords)):
				d += dist(coords[i], coords[j])
		if d < best_dist:
			best_dist = d
			best_coords = coords
	return zip(l, best_coords)

@logger.op("Aoa full on image {0} taken with {1} in {2}")
def aoa_full(camera, room, debug):
	#lights=[(([-500, 0]),((-5, 0, 0),)),(([500, 0]),((5, 0, 0),)),(([0, -500]),((0, -5, 0),)),(([0, 500]), ((0, 5, 0),)),(([5, 0]),((0, 0, 0),)),]
	#lights=[((-1803.1,-1594.5),((-0.5,0.0,0.0),)),((1988.0,-1377.0),((0.0,-0.5,0.0),)),((1.8,110.2),((0.0,0.0,0.0),)),((-1448.0,1129.5),((0.0,0.5,0.0),)),((1306.5,1264.5),((0.5,0.0,0.0),)),]
	lights = get_light();
	logger.debug('Raw lights information: {}'.format(lights))

	# Some frequencies have multiple locations, need to pick one
	lights = resolve_aliased_frequncies(lights)

	# AoA calcualation requires at least 3 transmitters
	assert len(lights) >= 3, "AoA calcualation requires at least 3 transmitters"

	tries = 3
	tries_rx_loc = numpy.empty([tries, 3])
	tries_rx_rot = numpy.empty([tries, 3, 3])
	tries_rx_err = numpy.empty([tries])
	tries_method = ['YS_brute', 'static', 'scipy_basin']
	tries_method = ['static']
	#tries_method = ['YS_brute']
	#tries_method = ['scipy_basin']
	#tries_method = ['scipy_brute']
	for i in xrange(tries):
		rx_location, rx_rotation, location_error = aoa(lights, camera.Zf, k_init_method=tries_method[i])
		logger.info('location estimate = {}'.format(rx_location))
		logger.info('location error    = {}'.format(location_error))
		tries_rx_loc[i] = rx_location
		tries_rx_rot[i] = rx_rotation
		tries_rx_err[i] = location_error

		if location_error > 1:
			if i == tries - 1:
				min_err_idx = numpy.argmin(tries_rx_err)
				rx_location = tries_rx_loc[min_err_idx]
				rx_rotation = tries_rx_rot[min_err_idx]
				location_error = tries_rx_err[min_err_idx]
				
				logger.info('Error ({}) too high, but max tries exceeded'.format(location_error))
				logger.warn('Returning measurement with high error estimate')
			else:
				logger.info('Error ({}) too high, trying again'.format(location_error))
		else:
			break

	'''
	for k in xrange(len(lights)):
		aoa_lights = [lights[k]]
		r = range(len(lights))
		r.pop(k)
		for i in r:
			aoa_lights.append(lights[i])
		logger.info('aoa_lights = {}'.format(aoa_lights))
		rx_location, rx_rotation, location_error = aoa(aoa_lights, camera.Zf)
		logger.info('location with brute  k_init = {}'.format(rx_location))
		logger.info('location error              = {}'.format(location_error))
	'''

	return (rx_location, rx_rotation, location_error)
