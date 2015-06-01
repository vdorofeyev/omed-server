package omed.cache


class CommonCacheService extends HazelcastCacheService {
  def decorateName(name: String) = name
}
