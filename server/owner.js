/**
 * KASA AI - Centralized Owner Access Service
 * 
 * Enforces server-side permanent owner access for authorized accounts.
 * Authorization is strictly based on the authenticated Firebase identity/email.
 * Client-supplied emails or bypass headers are never trusted.
 */

const OWNER_EMAILS = new Set([
  'powerkobbi9@gmail.com',
  'rabbiking713@gmail.com',
  'jakebenoit08@gmail.com',
]);

/**
 * Checks whether an email belongs to an authorized KASA AI owner.
 * Case-insensitive comparison.
 * 
 * @param {string} email
 * @returns {boolean}
 */
function isOwnerEmail(email) {
  if (!email || typeof email !== 'string') return false;
  return OWNER_EMAILS.has(email.trim().toLowerCase());
}

module.exports = {
  isOwnerEmail,
  OWNER_EMAILS,
};
