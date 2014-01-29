/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Qryop {

  protected List<Qryop> args = new ArrayList<Qryop>();
  
  /**
   * Evaluates the query operator, including any child operators and returns the result.
   * @return {@link QryResult} object
   * @throws IOException
   */
  public abstract QryResult evaluate() throws IOException;

  /**
   * Appends an argument to the list of query operator arguments.  This
   * simplifies the design of some query parsing architectures.
   * @return void
   * @throws IOException
   */
  public abstract void add(Qryop a) throws IOException;

}
