package com.liyunx.groot.template.freemarker;

import freemarker.template.*;

import java.util.*;

/**
 * ObjectWrapper 对象包装为 TemplateModel 类型。
 */
public class GrootObjectWrapper extends DefaultObjectWrapper {

  public GrootObjectWrapper(Version incompatibleImprovements) {
    super(incompatibleImprovements);
  }

  public TemplateModel wrap(Object obj) throws TemplateModelException {
    if (obj == null) {
      return super.wrap(null);
    }
    if (obj instanceof TemplateModel) {
      return (TemplateModel) obj;
    }
    if (obj instanceof String) {
      return new SimpleScalar((String) obj);
    }
    if (obj instanceof Number) {
      return new SimpleNumber((Number) obj);
    }
    if (obj instanceof Date) {
      if (obj instanceof java.sql.Date) {
        return new SimpleDate((java.sql.Date) obj);
      }
      if (obj instanceof java.sql.Time) {
        return new SimpleDate((java.sql.Time) obj);
      }
      if (obj instanceof java.sql.Timestamp) {
        return new SimpleDate((java.sql.Timestamp) obj);
      }
      return new SimpleDate((Date) obj, getDefaultDateType());
    }
    final Class<?> objClass = obj.getClass();
    if (objClass.isArray()) {
        return DefaultArrayAdapter.adapt(obj, this);
    }
    //if (obj instanceof Collection) {
    //
    //    if (obj instanceof List) {
    //      //return GrootListAdapter.adapt((List<?>) obj, this);
    //      return new SimpleSequence((List) obj, this);
    //    } else {
    //      return DefaultNonListCollectionAdapter.adapt((Collection<?>) obj, this);
    //    }
    //
    //}
    if (obj instanceof Map) {
      return GrootMapAdapter.adapt((Map<?, ?>) obj, this);
    }
    if (obj instanceof Boolean) {
      return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
    }
    if (obj instanceof Iterator) {
      return DefaultIteratorAdapter.adapt((Iterator<?>) obj, this);
    }
    if (obj instanceof Enumeration) {
      return DefaultEnumerationAdapter.adapt((Enumeration<?>) obj, this);
    }
    //if (obj instanceof Iterable) {
    //  return DefaultIterableAdapter.adapt((Iterable<?>) obj, this);
    //}

    return handleUnknownType(obj);
  }

}
