package omed.errors

import ru.atmed.omed.beans.model.meta.CompiledValidationRule

class ValidationException(val isValid: Boolean, val results: Seq[CompiledValidationRule]) extends Exception