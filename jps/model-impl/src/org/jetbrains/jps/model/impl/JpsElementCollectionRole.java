package org.jetbrains.jps.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.*;

/**
 * @author nik
 */
public class JpsElementCollectionRole<E extends JpsElement> extends JpsElementChildRoleBase<JpsElementCollectionImpl<E>>
                                                            implements JpsElementCreator<JpsElementCollectionImpl<E>> {
  private final JpsElementChildRole<E> myChildRole;

  private JpsElementCollectionRole(@NotNull JpsElementChildRole<E> role) {
    super("collection of " + role);
    myChildRole = role;
  }

  @NotNull
  @Override
  public JpsElementCollectionImpl<E> create() {
    return new JpsElementCollectionImpl<E>(myChildRole);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    return myChildRole.equals(((JpsElementCollectionRole)o).myChildRole);
  }

  @Override
  public int hashCode() {
    return myChildRole.hashCode();
  }

  public static <E extends JpsElement> JpsElementCollectionRole<E> create(@NotNull JpsElementChildRole<E> role) {
    return new JpsElementCollectionRole<E>(role);
  }
}
