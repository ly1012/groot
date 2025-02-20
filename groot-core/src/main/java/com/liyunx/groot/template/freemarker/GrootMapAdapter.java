package com.liyunx.groot.template.freemarker;

import freemarker.core._DelayedJQuote;
import freemarker.core._TemplateModelException;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.*;
import freemarker.template.utility.ObjectWrapperWithAPISupport;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

/**
 * 修改自 {@link DefaultMapAdapter}。
 *
 * Adapts a {@link Map} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateHashModelEx}. If you aren't wrapping an already existing {@link Map}, but build a hash specifically to
 * be used from a template, also consider using {@link SimpleHash} (see comparison there).
 *
 * <p>
 * Thread safety: A {@link DefaultMapAdapter} is as thread-safe as the {@link Map} that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these hashes (though of
 * course, Java methods called from the template can violate this rule).
 *
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 *
 * @since 2.3.22
 */
public class GrootMapAdapter extends WrappingTemplateModel
    implements TemplateHashModelEx2, AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport, TemplateScalarModel,
    Serializable {

  private final Map map;

  /**
   * Factory method for creating new adapter instances.
   *
   * @param map
   *            The map to adapt; can't be {@code null}.
   * @param wrapper
   *            The {@link ObjectWrapper} used to wrap the items in the array.
   */
  public static GrootMapAdapter adapt(Map map, ObjectWrapperWithAPISupport wrapper) {
    return new GrootMapAdapter(map, wrapper);
  }

  private GrootMapAdapter(Map map, ObjectWrapper wrapper) {
    super(wrapper);
    this.map = map;
  }

  @Override
  public TemplateModel get(String key) throws TemplateModelException {
    Object val;
    try {
      val = map.get(key);
    } catch (ClassCastException e) {
      throw new _TemplateModelException(e,
          "ClassCastException while getting Map entry with String key ",
          new _DelayedJQuote(key));
    } catch (NullPointerException e) {
      throw new _TemplateModelException(e,
          "NullPointerException while getting Map entry with String key ",
          new _DelayedJQuote(key));
    }

    if (val == null) {
      // Check for Character key if this is a single-character string.
      // In SortedMap-s, however, we can't do that safely, as it can cause ClassCastException.
      if (key.length() == 1 && !(map instanceof SortedMap)) {
        Character charKey = Character.valueOf(key.charAt(0));
        try {
          val = map.get(charKey);
          if (val == null) {
            TemplateModel wrappedNull = wrap(null);
            if (wrappedNull == null || !(map.containsKey(key) || map.containsKey(charKey))) {
              return null;
            } else {
              return wrappedNull;
            }
          }
        } catch (ClassCastException e) {
          throw new _TemplateModelException(e,
              "Class casting exception while getting Map entry with Character key ",
              new _DelayedJQuote(charKey));
        } catch (NullPointerException e) {
          throw new _TemplateModelException(e,
              "NullPointerException while getting Map entry with Character key ",
              new _DelayedJQuote(charKey));
        }
      } else {  // No char key fallback was possible
        TemplateModel wrappedNull = wrap(null);
        if (wrappedNull == null || !map.containsKey(key)) {
          return null;
        } else {
          return wrappedNull;
        }
      }
    }

    return wrap(val);
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public TemplateCollectionModel keys() {
    return new SimpleCollection(map.keySet(), getObjectWrapper());
  }

  @Override
  public TemplateCollectionModel values() {
    return new SimpleCollection(map.values(), getObjectWrapper());
  }

  @Override
  public KeyValuePairIterator keyValuePairIterator() {
    return new MapKeyValuePairIterator(map, getObjectWrapper());
  }

  @Override
  public Object getAdaptedObject(Class hint) {
    return map;
  }

  @Override
  public Object getWrappedObject() {
    return map;
  }

  @Override
  public TemplateModel getAPI() throws TemplateModelException {
    return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(map);
  }

  @Override
  public String getAsString() throws TemplateModelException {
    return map.toString();
  }
}

