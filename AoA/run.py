#!/usr/bin/env python2
# vim: sts=4 ts=4 sw=4 noet:

from __future__ import print_function
import sys
import os
import argparse
from aoa_full import aoa_full

import pretty_logger
logger = pretty_logger.get_logger()

if __name__ == '__main__':
	parser = argparse.ArgumentParser(
				formatter_class=argparse.RawDescriptionHelpFormatter,
				description='Program Action: Run image processing',
				epilog='''\
	Control debug level with DEBUG evinronment environment variable.
	  Default: no debugging
	  DEBUG=1: print debugging information
	  DEBUG=2: print debugging information and write out intermediate images to /tmp (slow)
	''')
	parser.add_argument('-f', '--filename', type=str,
			default='./jason.jpg',
			help='image to process')
	parser.add_argument('-c', '--camera', type=str,
			default='jason',
			help='phone type; must be in phones/')
	parser.add_argument('-m', '--method', type=str,
			default='opencv_fft',
			help='image processing method; must be in processors/')
	parser.add_argument('-r', '--room', type=str,
			default='jason',
			help='room the image was taken in; must be in rooms/')
	parser.add_argument('--only-image', action='store_true',
			help='stop after image processing (do not attempt localization)')
	parser.add_argument('-b','--box',action='store_true',
			help='Box light handleing')

	args = parser.parse_args()

	
	try:
		if os.environ['DEBUG'] == '2':
			debug = True
		else:
			debug = False
	except KeyError:
		debug = False

	try:
		#from phones import args.camera.split('-')[0] as phone
		phone = __import__('phones.' + args.camera.split('-')[0], fromlist=[1,])
	except ImportError:
		logger.error("Unknown phone: {}".format(args.camera.split('-')[0]))
		raise
	try:
		camera = getattr(phone, args.camera.split('-')[1])
	except IndexError:
		# A camera was not specified, use the default (elem 0) for this phone
		camera = phone.cameras[0]
	except AttributeError:
		# Found the phone, but not the specified camera
		logger.error("Unknown phone / camera combination")
		raise


	try:
		room = __import__('rooms.' + args.room, fromlist=[1,])
	except ImportError:
		logger.error('Unknown room')
		raise
	try:
		rx_location, rx_rotation, location_error = aoa_full(
				camera, room, debug)
		logger.info('rx_location = {}'.format(rx_location))
		logger.info('rx_rotation =\n{}'.format(rx_rotation))
		logger.info('location_error = {}'.format(location_error))
		logfile=open('log.txt','w')
		logfile.write('rx_location = {}\n'.format(rx_location))
		logfile.write('rx_rotation =\n{}\n'.format(rx_rotation))
		logfile.write('location_error = {}\n'.format(location_error))
		logfile.close()
		logfile=open('log_location.txt','w')
		logfile.write(format(rx_location))
		logfile.close()
	except Exception as e:
		logger.warn('Exception: {}'.format(e))
		raise
