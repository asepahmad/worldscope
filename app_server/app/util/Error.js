/**
 * Error module
 * @module app/util/Error
 */
var util = require('util');

var InvalidFieldError =
exports.InvalidFieldError = function(message, extra) {
  Error.captureStackTrace(this, this.constructor);
  this.name = 'InvalidFieldError';
  this.message = message;
  this.extra = extra;
};

var InvalidColumnError =
exports.InvalidColumnError = function(message) {
  Error.captureStackTrace(this, this.constructor);
  this.name = 'InvalidColumnError';
  this.message = message;
};

var NotFoundError =
exports.NotFoundError = function(message, details) {
  Error.captureStackTrace(this, this.constructor);
  this.name = 'NotFoundError';
  this.message = `${message} not found`;
  this.details = details;
};

var NotAuthorisedError =
exports.NotAuthorisedError = function(message) {
  Error.captureStackTrace(this, this.constructor);
  this.name = 'NotAuthorisedError';
  this.message = message;
};

var DuplicateEntryError =
exports.DuplicateEntryError = function(message) {
  Error.captureStackTrace(this, this.constructor);
  this.name = 'DuplicateEntryError';
  this.message = message;
};

var UnexpectedError =
exports.UnexpectedError = function(details) {
  Error.captureStackTrace(this, this.constructor);
  this.name = 'UnexpectedError';
  this.message = 'An unexpected error has occured';
  this.details = details;
};

util.inherits(InvalidFieldError, Error);
util.inherits(InvalidColumnError, Error);
util.inherits(NotFoundError, Error);
util.inherits(NotAuthorisedError, Error);
util.inherits(DuplicateEntryError, Error);
util.inherits(UnexpectedError, Error);

