package omed.bf

import omed.lang.eval.Configuration


trait ConfigurationProvider {

  def create(): Configuration

}
