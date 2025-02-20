package com.liyunx.groot.template.freemarker;

import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.*;
import freemarker.template.utility.ObjectWrapperWithAPISupport;
import freemarker.template.utility.RichObjectWrapper;

import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 修改自 {@link DefaultListAdapter}。
 *
 * <p>
 * Adapts a {@link List} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateSequenceModel}. If you aren't wrapping an already existing {@link List}, but build a sequence
 * specifically to be used from a template, also consider using {@link SimpleSequence} (see comparison there).
 * </p>
 *
 * <p>
 * Thread safety: A {@link DefaultListAdapter} is as thread-safe as the {@link List} that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these sequences (though
 * of course, Java methods called from the template can violate this rule).
 *
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 *
 * @see SimpleSequence
 * @see DefaultArrayAdapter
 * @see TemplateSequenceModel
 *
 * @since 2.3.22
 */
public class GrootListAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
    AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport, TemplateScalarModel, Serializable {

  protected final List list;

  /**
   * Factory method for creating new adapter instances.
   *
   * @param list
   *            The list to adapt; can't be {@code null}.
   * @param wrapper
   *            The {@link ObjectWrapper} used to wrap the items in the array.
   */
  public static GrootListAdapter adapt(List list, RichObjectWrapper wrapper) {
    // [2.4] DefaultListAdapter should implement TemplateCollectionModelEx, so this choice becomes unnecessary
    return list instanceof AbstractSequentialList
        ? new DefaultListAdapterWithCollectionSupport(list, wrapper)
        : new GrootListAdapter(list, wrapper);
  }

  private GrootListAdapter(List list, RichObjectWrapper wrapper) {
    super(wrapper);
    this.list = list;
  }

  @Override
  public TemplateModel get(int index) throws TemplateModelException {
    return index >= 0 && index < list.size() ? wrap(list.get(index)) : null;
  }

  @Override
  public int size() throws TemplateModelException {
    return list.size();
  }

  @Override
  public Object getAdaptedObject(Class hint) {
    return getWrappedObject();
  }

  @Override
  public Object getWrappedObject() {
    return list;
  }

  @Override
  public String getAsString() throws TemplateModelException {
    return list.toString();
  }

  private static class DefaultListAdapterWithCollectionSupport extends GrootListAdapter implements
      TemplateCollectionModel {

    private DefaultListAdapterWithCollectionSupport(List list, RichObjectWrapper wrapper) {
      super(list, wrapper);
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateModelException {
      return new IteratorToTemplateModelIteratorAdapter(list.iterator(), getObjectWrapper());
    }

  }

  @Override
  public TemplateModel getAPI() throws TemplateModelException {
    return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(list);
  }

}

class IteratorToTemplateModelIteratorAdapter implements TemplateModelIterator {

  private final Iterator<?> it;
  private final ObjectWrapper wrapper;

  IteratorToTemplateModelIteratorAdapter(Iterator<?> it, ObjectWrapper wrapper) {
    this.it = it;
    this.wrapper = wrapper;
  }

  @Override
  public TemplateModel next() throws TemplateModelException {
    try {
      return wrapper.wrap(it.next());
    } catch (NoSuchElementException e) {
      throw new TemplateModelException("The collection has no more items.", e);
    }
  }

  @Override
  public boolean hasNext() throws TemplateModelException {
    return it.hasNext();
  }

}


