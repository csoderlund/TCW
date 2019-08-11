package sng.assem.enums;

// Used in library loading
// WN this should have been set up using bitflags so there
// could be more than one action.
// However, UpdateProperties now means update either properties, or
// expression levels, or both, as appropriate.
public enum LibAction {
	NoAction, NewLoad, ReLoad,UpdateProperties, UpdateExpression
}
