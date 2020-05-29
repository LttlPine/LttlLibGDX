package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;

import com.lttlgames.editor.annotations.IgnoreCrawl;

@IgnoreCrawl
public class ProcessedFieldType
{
	private Type type;
	/**
	 * Only set if this processedFieldType came as a paramType from a class that had more than 1 param type. This saves
	 * it's declared letter.
	 */
	private String paramTypeLetter;
	private Class<?> currentClass;
	private ArrayList<ProcessedFieldType> params;
	private ProcessedFieldType[] paramTypesProcessed;
	private Field field;

	// CONSTRUCTORS
	/**
	 * Constructor used for creating processedFieldTypes with a currentType only, used for fitting into systems
	 * 
	 * @param currentType
	 */
	ProcessedFieldType(Class<?> currentType)
	{
		this.currentClass = currentType;
	}

	@SuppressWarnings("rawtypes")
	ProcessedFieldType(Type type, ProcessedFieldType... paramTypesProcessed)
	{
		this.type = type;
		this.paramTypesProcessed = paramTypesProcessed;

		// DEFINE THE CURRENT CLASS
		if (type.getClass() == Class.class)
		{
			// if the type is a class (ie. Vector2), then just cast it to a class
			currentClass = (Class<?>) type;
		}
		else if (ParameterizedType.class.isAssignableFrom(type.getClass()))
		{
			// if type is a parameterized type (ie. ArrayList<f>, HashMap<a,b> or AnyLttlClass<r>)
			currentClass = (Class<?>) ((ParameterizedType) type).getRawType();

			params = new ArrayList<ProcessedFieldType>();

			// add the params of the type as their own ProcessedClasses with the declaredClassparamType still
			Type[] paramTypes = ((ParameterizedType) type)
					.getActualTypeArguments();
			for (int i = 0; i < paramTypes.length; i++)
			{
				Type t = paramTypes[i];

				// ie <? extends CLASS>
				if (WildcardType.class.isAssignableFrom(t.getClass()))
				{
					t = ((WildcardType) t).getUpperBounds()[0];
				}

				ProcessedFieldType newPFT = new ProcessedFieldType(t,
						paramTypesProcessed);

				// if more than one param type, then set the paramTypeLetter
				if (paramTypes.length > 1)
				{
					newPFT.paramTypeLetter = currentClass.getTypeParameters()[i]
							.toString();
				}

				params.add(newPFT);
			}
		}
		else if (TypeVariable.class.isAssignableFrom(type.getClass())
				|| GenericArrayType.class.isAssignableFrom(type.getClass()))
		{
			// the type is using a typeVariable, make sure the paramTypeProcessed has some types in it
			if (paramTypesProcessed.length == 0)
			{
				if (field != null)
				{
					Lttl.Throw("Field "
							+ field.getName()
							+ " on "
							+ field.getDeclaringClass().getSimpleName()
							+ " is using a param type that is not being defined in a subclass.");
				}
				else
				{
					Lttl.Throw("There is an undefined param type in an ArrayList or Hashmap or something.");
				}
			}

			int paramIndex = 0;
			if (paramTypesProcessed.length > 1)
			{
				String paramTypeLetter;
				if (TypeVariable.class.isAssignableFrom(type.getClass()))
				{
					paramTypeLetter = ((TypeVariable) type).getName();
				}
				else
				{
					paramTypeLetter = ((TypeVariable) ((GenericArrayType) type)
							.getGenericComponentType()).getName();
				}

				// now find the correct paramType index by comparing the param type letter
				for (ProcessedFieldType gtp : paramTypesProcessed)
				{
					if (gtp.paramTypeLetter != null
							&& paramTypeLetter.equals(gtp.paramTypeLetter))
					{
						break;
					}
					paramIndex++;
				}
			}

			// set this PFT to the paramType at the index with the same name, or first, if only one
			// since this field is the same type of the paramType, we can just set it as it
			Lttl.Throw(
					paramTypesProcessed[paramIndex] == null,
					"Trying to dump an object that has a <T> type variable but there are no processed params.  Try dumping the object that has this object.");
			set(paramTypesProcessed[paramIndex]);

			// modify the class for a param type array
			if (GenericArrayType.class.isAssignableFrom(type.getClass()))
			{
				// check if it's an array using the type variable (T[])
				int dimensions = 1;
				// iterate through the component type to see how many dimensions there are
				Type iterationType = type;
				while (GenericArrayType.class
						.isAssignableFrom((iterationType = ((GenericArrayType) iterationType)
								.getGenericComponentType()).getClass()))
				{
					dimensions++;
				}
				// create an instance so can derive class from it
				currentClass = Array.newInstance(getCurrentClass(),
						new int[dimensions]).getClass();
			}
		}
		else
		{
			// if neither, error out
			Lttl.Throw("The type [" + type.getClass().getSimpleName()
					+ "]  is neither a type variable or a class...");
		}
	}

	/**
	 * @param field
	 * @param paramTypeProcessed
	 */
	public ProcessedFieldType(Field field,
			ProcessedFieldType... paramTypesProcessed)
	{
		this(field.getGenericType(), paramTypesProcessed);
		this.field = field;
	}

	/**
	 * Returns the processFieldType for the parameter at index
	 * 
	 * @param index
	 * @return null if index out of range or no params
	 */
	public ProcessedFieldType getParam(int index)
	{
		if (params == null || params.size() <= index) return null;
		return params.get(index);
	}

	/**
	 * Returns the first processFieldType for the parameter
	 * 
	 * @return null if none
	 */
	public ProcessedFieldType getParam()
	{
		return getParam(0);
	}

	private ProcessedFieldType[] paramsCache;

	/**
	 * Gets all the params.
	 * 
	 * @return
	 */
	public ProcessedFieldType[] getParams()
	{
		if (paramsCache == null)
		{
			paramsCache = new ProcessedFieldType[getParamCount()];
			if (params != null)
			{
				int i = 0;
				for (ProcessedFieldType p : params)
				{
					paramsCache[i] = p;
					i++;
				}
			}
		}
		return paramsCache;
	}

	public int getParamCount()
	{
		if (params == null) return 0;
		return params.size();
	}

	/**
	 * Returns the actual class of this field or processed parameter, since it could be from a param type (T)
	 * 
	 * @return
	 */
	public Class<?> getCurrentClass()
	{
		return currentClass;
	}

	public Type getType()
	{
		return type;
	}

	public Field getField()
	{
		return field;
	}

	/**
	 * Sets the params, current Type and param types from the provided pft
	 * 
	 * @param pft
	 */
	private void set(ProcessedFieldType pft)
	{
		params = pft.params;
		currentClass = pft.currentClass;
		paramTypesProcessed = pft.paramTypesProcessed;
	}
}
